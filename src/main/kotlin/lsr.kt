import com.microsoft.playwright.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun main() {
    Playwright.create().use { playwright ->
        val browser = playwright.chromium().launch(
            BrowserType.LaunchOptions().setHeadless(true)
        )

        val context = browser.newContext(
            Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36")
                .setLocale("ru-RU")
                .setExtraHTTPHeaders(mapOf("Accept-Language" to "ru-RU,ru;q=0.9"))
        )

        val page = context.newPage()
        page.navigate("https://www.lsr.ru/spb/zhilye-kompleksy/tsvetnoy-gorod-pejzazhnyj-kvartal/")
        page.waitForTimeout(3000.0)
        page.keyboard().press("PageDown")
        page.waitForTimeout(3000.0)

        val buildingCards = page.locator("div.buildingCard")
        val count = buildingCards.count()
        println("🔍 Найдено $count карточек корпусов\n")

        val buildings = mutableListOf<Pair<String, Int>>()
        for (i in 0 until count) {
            val card = buildingCards.nth(i)
/*
            val texts = card.locator("p.label.l1.bold")
            val tCount = texts.count()
            println("Карточка $i: найдено $tCount элементов <p.label.l1.bold>")
            for (j in 0 until tCount) {
                val text = texts.nth(j).textContent()?.trim()
                println("  $j: $text")
            }
*/
            val paragraphs = card.locator("p.label.l1.bold")
            val countP = paragraphs.count()

            val rawName = (0 until countP)
                .map { paragraphs.nth(it).textContent()?.trim() ?: "" }
                .firstOrNull { it.lowercase().startsWith("дом") || it.matches(Regex("^\\d+$")) }
                ?: "(неизвестно)"

            val name = rawName.replace(Regex("^дом\\s*", RegexOption.IGNORE_CASE), "")
            val flats = card.locator("span.buildingCard__count").first().textContent()?.trim()?.toIntOrNull() ?: 0
            buildings.add(name to flats)
            println("🏢 $name — $flats квартир")
        }

        if (buildings.isNotEmpty()) {
            val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            saveToDatabase(currentDate, buildings)
            printStatsFromDatabase()
        } else {
            println("\n🚫 Нет данных для сохранения.")
        }

        browser.close()
    }
}

fun saveToDatabase(date: String, data: List<Pair<String, Int>>) {
    Class.forName("org.sqlite.JDBC")
    val dbUrl = "jdbc:sqlite:buildings.db"
    val connection: Connection = DriverManager.getConnection(dbUrl)

    connection.createStatement().use { stmt ->
        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS buildings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                building TEXT NOT NULL,
                flats INTEGER NOT NULL,
                UNIQUE(date, building)
            )
            """.trimIndent()
        )
    }

    val insertSQL = """
        INSERT INTO buildings(date, building, flats)
        VALUES(?, ?, ?)
        ON CONFLICT(date, building) DO UPDATE SET flats = excluded.flats
    """.trimIndent()

    val preparedStatement: PreparedStatement = connection.prepareStatement(insertSQL)

    for ((building, flats) in data) {
        preparedStatement.setString(1, date)
        preparedStatement.setString(2, building)
        preparedStatement.setInt(3, flats)
        preparedStatement.addBatch()
    }

    preparedStatement.executeBatch()
    println("\n✅ Данные сохранены/обновлены в базе.")
    connection.close()
}



fun printStatsFromDatabase() {
    Class.forName("org.sqlite.JDBC")
    val dbUrl = "jdbc:sqlite:buildings.db"
    val connection = DriverManager.getConnection(dbUrl)

    val query = """
        SELECT date, building, flats
        FROM buildings
        ORDER BY building, date
    """.trimIndent()

    val statsMap = mutableMapOf<String, MutableList<Pair<String, Int>>>()

    connection.createStatement().use { stmt ->
        val rs = stmt.executeQuery(query)
        while (rs.next()) {
            val date = rs.getString("date")
            val building = rs.getString("building")
            val flats = rs.getInt("flats")
            statsMap.computeIfAbsent(building) { mutableListOf() }.add(date to flats)
        }
    }

    println("\n📊 Статистика по корпусам:")

    for ((building, records) in statsMap) {
        println("\n🏢 Корпус $building:")
        var prevFlats: Int? = null
        for ((date, flats) in records) {
            val diff = prevFlats?.let { flats - it }
            val diffText = when {
                diff == null -> ""
                diff > 0 -> " (+$diff)"
                diff < 0 -> " ($diff)"
                else -> " (0)"
            }
            println("  $date — $flats квартир$diffText")
            prevFlats = flats
        }
    }

    connection.close()
}


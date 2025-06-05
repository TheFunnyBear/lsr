import org.jsoup.Jsoup
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun main() {
    val url = "https://www.lsr.ru/spb/zhilye-kompleksy/tsvetnoy-gorod-pejzazhnyj-kvartal/"
    val buildings = mutableMapOf<String, Int>()
    val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

    try {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .get()

        val buildingCards = doc.select("div.buildingCard")

        val buildingRegex = Regex("""дом\s*([\dА-Яа-яA-Za-z]+)""", RegexOption.IGNORE_CASE)

        for (card in buildingCards) {
            val nameElement = card.selectFirst("p.label.l1.bold")
            val rawName = nameElement?.text()?.trim() ?: "Неизвестно"

            val matchResult = buildingRegex.find(rawName)
            val buildingNum = matchResult?.groupValues?.get(1) ?: rawName

            val countElement = card.selectFirst("span.buildingCard__count")
            val flatsCount = countElement?.text()?.trim()?.toIntOrNull() ?: 0

            buildings[buildingNum] = buildings.getOrDefault(buildingNum, 0) + flatsCount
        }

        val sortedBuildings = buildings.toList()
            .sortedWith(compareBy({ sortKey(it.first).first }, { sortKey(it.first).second }))

        println("Количество квартир в продаже по корпусам на $currentDate\n")
        for ((building, totalFlats) in sortedBuildings) {
            println("Корпус: $building, Квартир в продаже: $totalFlats")
        }

        saveToDatabase(currentDate, sortedBuildings)
        printStatsFromDatabase()
        // printStatsByDate("2025-06-05") // Раскомментируй, чтобы посмотреть статистику на конкретную дату

    } catch (e: Exception) {
        println("Ошибка при получении данных: ${e.message}")
    }
}

fun sortKey(building: String): Pair<Int, String> {
    val regex = Regex("""(\d+)([^\d]*)""")
    val match = regex.find(building)
    return if (match != null) {
        val numPart = match.groupValues[1].toInt()
        val letterPart = match.groupValues[2].uppercase(Locale.getDefault())
        numPart to letterPart
    } else {
        9999 to ""
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
                flats INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    val insertSQL = "INSERT INTO buildings(date, building, flats) VALUES(?, ?, ?)"
    val preparedStatement: PreparedStatement = connection.prepareStatement(insertSQL)

    for ((building, flats) in data) {
        preparedStatement.setString(1, date)
        preparedStatement.setString(2, building)
        preparedStatement.setInt(3, flats)
        preparedStatement.addBatch()
    }

    preparedStatement.executeBatch()
    println("\nДанные успешно сохранены в локальную базу данных.")
    connection.close()
}

fun printStatsFromDatabase() {
    Class.forName("org.sqlite.JDBC")
    val dbUrl = "jdbc:sqlite:buildings.db"
    val connection = DriverManager.getConnection(dbUrl)

    println("\n📅 Все сохранённые записи:")
    val allQuery = "SELECT date, building, flats FROM buildings ORDER BY date, building"
    connection.createStatement().use { stmt ->
        val rs = stmt.executeQuery(allQuery)
        while (rs.next()) {
            val date = rs.getString("date")
            val building = rs.getString("building")
            val flats = rs.getInt("flats")
            println("$date | Корпус $building: $flats квартир")
        }
    }

    connection.close()
}

fun printStatsByDate(date: String) {
    Class.forName("org.sqlite.JDBC")
    val dbUrl = "jdbc:sqlite:buildings.db"
    val connection = DriverManager.getConnection(dbUrl)

    println("\n📆 Статистика на $date:")
    val query = "SELECT building, flats FROM buildings WHERE date = ? ORDER BY building"

    val stmt = connection.prepareStatement(query)
    stmt.setString(1, date)
    val rs = stmt.executeQuery()

    while (rs.next()) {
        val building = rs.getString("building")
        val flats = rs.getInt("flats")
        println("Корпус $building: $flats квартир")
    }

    connection.close()
}

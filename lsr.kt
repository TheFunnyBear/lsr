import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun main() {
    val url = "https://www.lsr.ru/spb/zhilye-kompleksy/tsvetnoy-gorod-pejzazhnyj-kvartal/"
    val buildings = mutableMapOf<String, Int>()

    try {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                       "(KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
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

        // Функция сортировки корпуса по числу и букве
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

        val sortedBuildings = buildings.toList()
            .sortedWith(compareBy({ sortKey(it.first).first }, { sortKey(it.first).second }))

        val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        println("Количество квартир в продаже по корпусам на $currentDate\n")

        for ((building, totalFlats) in sortedBuildings) {
            println("Корпус: $building, Квартир в продаже: $totalFlats")
        }

    } catch (e: Exception) {
        println("Ошибка при получении данных: ${e.message}")
    }
}

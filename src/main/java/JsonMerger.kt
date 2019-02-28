import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.File
import java.io.FileReader

class JsonMerger() {
    val mockJson2 = """
{
  "siteTitle": "AirTours | Package Holidays, Hotels and Flights, Cheap holidays",
  "common": {
    "days": "Days",
    "nights": "Nights",
    "room": "Room",
    "interstitialMessage": "Please wait while we create your perfect holiday, this may take a few moments...",
    "from": "From",
    "to": "to",
    "on": "on"
  }
}
""".trimIndent()

    val mockJson3 = """
{
  "siteTitle": "AirTours | Package Holidays, Hotels and Flights, Cheap holidays",
  "common": {
        "potato": "on"
  }
}

""".trimIndent()

    val mockJson4 = """
{
  "siteTitle": "AirTours | Package Holidays, Hotels and Flights, Cheap holidays",
  "common": {
        "potato": "anotherpotato"
  }
}

""".trimIndent()
    val result: HashMap<String, Any> = hashMapOf()

    fun loadJson(path: String, names: List<String>): HashMap<String, Any> {
//        System.out.println("gettin");
        for ((index, name) in names.withIndex()) {
            getMap(index, "", result, JsonParser().parse(FileReader(File("$path/$name.json"))))
        }
//        getMap("", result, JsonParser().parse(mockJson3))
//        getMap("", result, JsonParser().parse(mockJson4))
        return result;
    }

    fun getMap(position: Int, path: String, parent: HashMap<String, Any>, jsonElement: JsonElement) {
        if (jsonElement.isJsonObject) {
            val jsonObject = jsonElement.asJsonObject
            for ((key, value) in jsonObject.entrySet()) {
                if (value.isJsonObject) {
                    val objectNode = parent.get(key) ?: hashMapOf<String, Any>()
                    if (objectNode is ArrayList<*>) {
                        return;
                    }
                    objectNode as HashMap<String, Any>
//                    System.out.println("System called")
                    parent.set(key, objectNode)
                    getMap(position, "" + path + "/" + key, objectNode, value)
                }
                if (value.isJsonPrimitive) {
//                    System.out.println("System primitive")
//                    System.out.println(value.asJsonPrimitive.asString)
//                    System.out.println(parent.get(key).toString())
                    val listnode = parent.get(key) ?: arrayListOf<String>()
//                    System.out.println(listnode)
                    if (listnode is HashMap<*, *>) {
                        return;
                    }
                    listnode as ArrayList<String>
                    if (listnode.size == 0)
                        listnode.add(path + "/" + key)
                    while(listnode.size < position +1){
                        listnode.add("")
                    }
                    listnode.add(position + 1, value.asJsonPrimitive.asString)
                    parent.set(key, listnode)
                }
            }
        }
    }
}
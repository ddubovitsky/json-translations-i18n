import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import javafx.application.Platform
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.embed.swing.JFXPanel
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.paint.Color
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.PrintStream
import javafx.scene.layout.HBox
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent


val mockJson = """
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

class TestToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val fxPanel = JFXPanel()
        val component = toolWindow.component
        Platform.setImplicitExit(false)
        Platform.runLater {
            val rootScene = Group()
            val width = 1200
            val scene = Scene(rootScene, Color.ALICEBLUE)
            rootScene.children.add(renderPathInput(rootScene, project, width))
            fxPanel.scene = scene
        }

        component.parent.add(fxPanel)
    }
}

fun rerender(rootScene: Group, project: Project, width: Int, path: String){
    val basePath = project.basePath + path
    System.out.println("toolWindow.component.width = " + width)
    val names = File(basePath).listFiles().map { it.nameWithoutExtension }.filter { !it.isEmpty() }
    System.out.println(project.basePath)
    val treeView = renderTreeView(1f * width * 0.3, names, basePath)
    treeView.prefWidth = 1f * width * 0.3

    val tableView = renderTableView((1f * width * 0.7) / names.size, names, treeView, basePath)
    tableView.prefWidth = 1f * width * 0.7
    tableView.layoutX = 1f * width * 0.3

    rootScene.children.add(treeView)
    rootScene.children.add(tableView)
    rootScene.children.add(renderPathInput(rootScene, project, width))
}

fun renderPathInput(rootScene: Group, project: Project, width: Int): HBox{
    val label1 = Label("Path To Styles:")
    val textField = TextField()
    textField.onKeyPressed = object : EventHandler<KeyEvent> {
        override fun handle(ke: KeyEvent) {
            if (ke.code.equals(KeyCode.ENTER)) {
                rerender(rootScene, project, width, textField.text)
            }
        }
    }
    val hb = HBox()
    hb.children.addAll(label1, textField)
    hb.spacing = 10.0
    return hb
}

fun renderTreeView(columnWidth: Double, names: List<String>, basePath: String): TreeTableView<ObjectNode> {
    val column1 = TreeTableColumn<ObjectNode, String>("Column")
    column1.prefWidth = columnWidth
    column1.isResizable = true
    column1.setCellValueFactory { p: TreeTableColumn.CellDataFeatures<ObjectNode, String> -> ReadOnlyStringWrapper(p.value.value.name) }
    val treeTableView = TreeTableView(getTreeItem(basePath, names))
    treeTableView.columns.addAll(column1)
    treeTableView.isShowRoot = true


    treeTableView.selectionModel.selectedCells.addListener(ListChangeListener {
        while (it.next()) {
            val column = treeTableView.selectionModel.selectedCells[0].column
            System.out.println("path =" + it.list[0].treeItem.value.path)
            data.setAll(
                    arrayListOf(it.list[0].treeItem.value.translations)
            )
            System.out.println("Column = $column")
        }
    })
    return treeTableView
}

fun renderTableView(columnWidth: Double, names: List<String>, treeTableView: TreeTableView<ObjectNode>, basePath: String): TableView<ArrayList<String>> {
    val table = TableView<ArrayList<String>>()
    table.isEditable = true
    for ((index, name) in names.withIndex()) {
        val column = TableColumn<ArrayList<String>, String>(name)
        column.isEditable = true
        column.isResizable = true
        column.prefWidth = columnWidth
        column.cellFactory = TextFieldTableCell.forTableColumn()

        column.setOnEditCommit {
            System.out.println("commit update")
            treeTableView.selectionModel.selectedCells[0].treeItem.value.translations[index] = it.newValue
            val path = treeTableView.selectionModel.selectedCells[0].treeItem.value.path
            modifyAtPath("" + basePath + "/$name.json", path, it.newValue)
        }

        table.columns.add(column)
        column.setCellValueFactory { param ->
            ReadOnlyObjectWrapper<String>(param.value.getOrElse(index, { "" }))
        }
    }


    data.addListener(ListChangeListener {
        table.items.setAll(it.list)
    })
//    table.items.addAll(data)
    return table
}

fun getTreeItem(basePath: String, names: List<String>): TreeItem<ObjectNode> {
    val root = TreeItem(ObjectNode("", "Root"))
    root.isExpanded = true
    root.children.setAll(getTreeItemForJsonElement(JsonMerger().loadJson(basePath, names)))
    return root
}


fun getTreeItemForJsonElement(hashMap: HashMap<String, Any>): List<TreeItem<ObjectNode>> {
    val list = arrayListOf<TreeItem<ObjectNode>>()

    for ((key, value) in hashMap) {
        val objectNode = ObjectNode("", key)
        val currentTreeItem = TreeItem<ObjectNode>(objectNode)
        if (value is ArrayList<*>) {
            currentTreeItem.value.path = value[0] as String
            currentTreeItem.value.translations.addAll((value as ArrayList<String>).subList(1, value.size))
        } else {
            currentTreeItem.children.setAll(getTreeItemForJsonElement(value as HashMap<String, Any>))
        }
        list.add(currentTreeItem)
    }

    return list
}

val data: ObservableList<ArrayList<String>> = FXCollections.observableArrayList<ArrayList<String>>(
        arrayListOf("Jacob", ""),
        arrayListOf("Isabella", "Johnson", "isabella.johnson@example.com"),
        arrayListOf("Ethan", "Williams", "ethan.williams@example.com"),
        arrayListOf("Emma", "Jones", "emma.jones@example.com"),
        arrayListOf("Michael", "Brown", "michael.brown@example.com")
)

class ObjectNode(
        var path: String,
        val name: String,
        val translations: ArrayList<String> = arrayListOf()
)

fun modifyAtPath(filePath: String, path: String, modifyto: String) {

    val someFile = File(filePath)
    val temp = File.createTempFile(someFile.name, null)
    var reader: BufferedReader? = null
    var writer: PrintStream? = null
    var listPath = ArrayList(path.split("/"))
    try {
        reader = BufferedReader(FileReader(someFile))
        writer = PrintStream(temp)

        var line: String? = reader.readLine()
        while (line != null) {
            // manipulate line
            if (listPath.size != 0 && line.contains(listPath[0])) {
                val key = listPath.removeAt(0)
                if (listPath.size == 0) {
                    line = replaceForKey(line, key, modifyto)
                }
            }
            writer.println(line)
//            System.out.println(line);
            line = reader.readLine()
        }
    } finally {
        writer?.close()
        reader?.close()
    }
    if (!someFile.delete()) throw Exception("Failed to remove " + someFile.name)
    if (!temp.renameTo(someFile)) throw Exception("Failed to replace " + someFile.name)
}

fun replaceForKey(string: String, key: String, value: String): String {
    return string.replace(Regex("""(?<=${key}).*""""), "\": \"${value}\"")
}

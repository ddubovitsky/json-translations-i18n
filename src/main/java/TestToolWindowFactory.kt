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
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.paint.Color
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.PrintStream

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
            val scene = Scene(rootScene, Color.ALICEBLUE)
            val basePath = project.basePath + "/i18n"
            val names = File(basePath).listFiles().map { it.nameWithoutExtension }.filter { !it.isEmpty() }
            System.out.println(project.basePath);
            val treeView = renderTreeView(1f * toolWindow.component.width * 0.3, names, basePath)
            treeView.prefWidth = 1f * toolWindow.component.width * 0.3

            val tableView = renderTableView((1f * toolWindow.component.width * 0.7) / names.size, names, treeView, basePath)
            tableView.prefWidth = 1f * toolWindow.component.width * 0.7
            tableView.layoutX = 1f * toolWindow.component.width * 0.3

            rootScene.getChildren().add(treeView);
            rootScene.getChildren().add(tableView);
            fxPanel.scene = scene
        }

        component.parent.add(fxPanel)
    }
}

fun renderTreeView(columnWidth: Double, names: List<String>, basePath: String): TreeTableView<ObjectNode> {
    val column1 = TreeTableColumn<ObjectNode, String>("Column")
    column1.prefWidth = columnWidth
    column1.isResizable = true;
    column1.setCellValueFactory { p: TreeTableColumn.CellDataFeatures<ObjectNode, String> -> ReadOnlyStringWrapper(p.getValue().getValue().name) }
    val treeTableView = TreeTableView(getTreeItem(basePath, names))
    treeTableView.getColumns().addAll(column1)
    treeTableView.setShowRoot(true)


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
    table.isEditable = true;
    for ((index, name) in names.withIndex()) {
        val column = TableColumn<ArrayList<String>, String>(name)
        column.isEditable = true;
        column.isResizable = true;
        column.prefWidth = columnWidth;
        column.setCellFactory(
                TextFieldTableCell.forTableColumn());

        column.setOnEditCommit {
            System.out.println("commit update");
            treeTableView.selectionModel.selectedCells[0].treeItem.value.translations[index] = it.newValue
            val path = treeTableView.selectionModel.selectedCells[0].treeItem.value.path
            modifyAtPath("" + basePath + "/$name.json", path, it.newValue)
        }

        table.getColumns().add(column);
        column.setCellValueFactory { param ->
            ReadOnlyObjectWrapper<String>(param.getValue().getOrElse(index, { "" }))
        }
    }


    data.addListener(ListChangeListener {
        table.items.setAll(it.list);
    })
//    table.items.addAll(data)
    return table;
}

fun getTreeItem(basePath: String, names: List<String>): TreeItem<ObjectNode> {
    val root = TreeItem(ObjectNode("", "Root"))
    root.isExpanded = true
    root.getChildren().setAll(getTreeItemForJsonElement(JsonMerger().loadJson(basePath, names)));
    return root;
}


fun getTreeItemForJsonElement(hashMap: HashMap<String, Any>): List<TreeItem<ObjectNode>> {
    val list = arrayListOf<TreeItem<ObjectNode>>()

    for ((key, value) in hashMap) {
        val objectNode = ObjectNode("", key);
        val currentTreeItem = TreeItem<ObjectNode>(objectNode);
        if (value is ArrayList<*>) {
            currentTreeItem.value.path = value[0] as String;
            currentTreeItem.value.translations.addAll((value as ArrayList<String>).subList(1, value.size))
        } else {
            currentTreeItem.children.setAll(getTreeItemForJsonElement(value as HashMap<String, Any>))
        }
        list.add(currentTreeItem)
    }

    return list;
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
                val key = listPath.removeAt(0);
                if (listPath.size == 0) {
                    line = replaceForKey(line, key, modifyto)
                }
            }
            writer.println(line)
            System.out.println(line);
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

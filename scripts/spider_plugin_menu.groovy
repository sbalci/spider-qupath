// Updated SPIDER_Plugin_Menu.groovy
// Fixed stray duplicate block and ensured proper brace balance
// Adjusted to correctly locate Python scripts under the project directory

import javafx.application.Platform
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.geometry.Insets
import javafx.stage.Stage
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.scene.input.KeyCombination
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.gui.scripting.QPEx
import qupath.lib.scripting.QP
import qupath.lib.io.GsonTools
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

// Configuration class to store user preferences
class SPIDERConfig {
    static String pythonPath = ""
    static String modelsBasePath = ""
    static String lastSelectedModel = "colorectal"
    static boolean showTutorial = true
    static void save() {
        def configFile = new File(QPEx.buildFilePath(QPEx.PROJECT_BASE_DIR, "config/spider_config.json"))
        def config = [
            pythonPath: pythonPath,
            modelsBasePath: modelsBasePath,
            lastSelectedModel: lastSelectedModel,
            showTutorial: showTutorial
        ]
        def gson = GsonTools.getInstance(true)
        configFile.text = gson.toJson(config)
    }
    static void load() {
        def configFile = new File(QPEx.buildFilePath(QPEx.PROJECT_BASE_DIR, "config/spider_config.json"))
        if (configFile.exists()) {
            def parser = new JsonParser()
            def jsonObj = parser.parse(configFile.text).getAsJsonObject()
            pythonPath = jsonObj.has("pythonPath") ? jsonObj.get("pythonPath").getAsString() : ""
            modelsBasePath = jsonObj.has("modelsBasePath") ? jsonObj.get("modelsBasePath").getAsString() : ""
            lastSelectedModel = jsonObj.has("lastSelectedModel") ? jsonObj.get("lastSelectedModel").getAsString() : "colorectal"
            showTutorial = jsonObj.has("showTutorial") ? jsonObj.get("showTutorial").getAsBoolean() : true
        }
    }
}

// Load configuration
SPIDERConfig.load()

def getModelInfo() {
    return [
        colorectal: [
            name: "SPIDER Colorectal",
            fullName: "SPIDER-colorectal-model",
            description: "13 classes for colorectal tissue analysis",
            classes: ["Adenocarcinoma high grade", "Adenocarcinoma low grade", "Adenoma high grade", 
                      "Adenoma low grade", "Fat", "Hyperplastic polyp", "Inflammation", "Mucus", 
                      "Muscle", "Necrosis", "Sessile serrated lesion", "Stroma healthy", "Vessels"],
            accuracy: "91.4%"
        ],
        skin: [
            name: "SPIDER Skin",
            fullName: "SPIDER-skin-model",
            description: "24 classes for skin pathology analysis",
            classes: ["Basal Cell Carcinoma", "Melanoma invasive", "Melanoma in situ", 
                      "Squamous Cell Carcinoma", "Epidermis", "Vessels", "etc."],
            accuracy: "94.0%"
        ],
        thorax: [
            name: "SPIDER Thorax",
            fullName: "SPIDER-thorax-model",
            description: "14 classes for thoracic tissue analysis",
            classes: ["Small cell carcinoma", "Non-small cell carcinoma", "Alveoli", 
                      "Vessels", "etc."],
            accuracy: "96.2%"
        ]
    ]
}

def createMainMenu() {
    def modelInfo = getModelInfo()
    Platform.runLater {
        def stage = new Stage()
        stage.setTitle("SPIDER Pathology Analysis")
        def mainPane = new VBox(15)
        mainPane.setPadding(new Insets(20))
        mainPane.setStyle("-fx-background-color: #f0f0f0;")

        def headerLabel = new Label("SPIDER Digital Pathology Analysis")
        headerLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;")
        def subheaderLabel = new Label("AI-powered tissue classification for pathology")
        subheaderLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;")

        def modelBox = new VBox(10)
        def modelLabel = new Label("Select Model:")
        modelLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;")
        def modelChoiceBox = new ChoiceBox<String>()
        modelChoiceBox.getItems().addAll("Colorectal", "Skin", "Thorax")
        modelChoiceBox.setValue(SPIDERConfig.lastSelectedModel.capitalize())
        modelChoiceBox.setStyle("-fx-pref-width: 300px;")
        def modelDescLabel = new Label()
        modelDescLabel.setWrapText(true)
        modelDescLabel.setStyle("-fx-text-fill: #555555;")
        def updateModelDesc = {
            def selected = modelChoiceBox.getValue().toLowerCase()
            def info = modelInfo[selected]
            modelDescLabel.setText("${info.description}\nAccuracy: ${info.accuracy}")
            SPIDERConfig.lastSelectedModel = selected
        }
        modelChoiceBox.setOnAction { updateModelDesc() }
        updateModelDesc()
        modelBox.getChildren().addAll(modelLabel, modelChoiceBox, modelDescLabel)

        def buttonBox = new VBox(10)
        buttonBox.setStyle("-fx-padding: 20 0 0 0;")
        def createStyledButton = { text, action ->
            def button = new Button(text)
            button.setStyle("""
                -fx-pref-width: 300px;
                -fx-pref-height: 40px;
                -fx-font-size: 14px;
                -fx-background-color: #3498db;
                -fx-text-fill: white;
                -fx-cursor: hand;
            """
            )
            button.setOnMouseEntered { 
                button.setStyle(button.getStyle() + "-fx-background-color: #2980b9;") 
            }
            button.setOnMouseExited { 
                button.setStyle(button.getStyle().replace("-fx-background-color: #2980b9;", "-fx-background-color: #3498db;"))
            }
            button.setOnAction(action)
            return button
        }
        def classifyAnnotationsBtn = createStyledButton("1. Classify Selected Annotations") {
            stage.close()
            runClassifyAnnotations(modelChoiceBox.getValue().toLowerCase())
        }
        def tileAnalysisBtn = createStyledButton("2. Tile Analysis (Detailed)") {
            stage.close()
            runTileAnalysis(modelChoiceBox.getValue().toLowerCase())
        }
        def wholeSlideBtn = createStyledButton("3. Whole Slide Analysis") {
            stage.close()
            showWholeSlideDialog(modelChoiceBox.getValue().toLowerCase())
        }
        def tutorialBtn = createStyledButton("Show Tutorial") {
            showTutorial()
        }
        def configBtn = new Button("⚙ Configuration")
        configBtn.setStyle("""
            -fx-pref-width: 150px;
            -fx-background-color: #95a5a6;
            -fx-text-fill: white;
        """
        )
        configBtn.setOnAction {
            showConfigurationDialog()
        }
        buttonBox.getChildren().addAll(
            classifyAnnotationsBtn,
            tileAnalysisBtn,
            wholeSlideBtn,
            new Separator(),
            tutorialBtn,
            configBtn
        )

        def helpText = new TextArea()
        helpText.setText("""Quick Start Guide:
1. First, configure Python path and model locations
2. Draw annotations on your slide
3. Select annotations and choose classification method
4. View results with color-coded classifications""")
        helpText.setEditable(false)
        helpText.setWrapText(true)
        helpText.setPrefRowCount(4)
        helpText.setStyle("-fx-font-size: 12px;")

        mainPane.getChildren().addAll(
            headerLabel,
            subheaderLabel,
            new Separator(),
            modelBox,
            buttonBox,
            new Separator(),
            helpText
        )
        def scene = new javafx.scene.Scene(mainPane, 400, 600)
        stage.setScene(scene)
        stage.setResizable(false)
        stage.show()

        if (SPIDERConfig.showTutorial) {
            showTutorial()
            SPIDERConfig.showTutorial = false
            SPIDERConfig.save()
        }
    }
}

def showConfigurationDialog() {
    Platform.runLater {
        def dialog = new Dialog<ButtonType>()
        dialog.setTitle("SPIDER Configuration")
        dialog.setHeaderText("Configure SPIDER Settings")
        def grid = new GridPane()
        grid.setHgap(10)
        grid.setVgap(10)
        grid.setPadding(new Insets(20))

        def pythonField = new TextField(SPIDERConfig.pythonPath)
        pythonField.setPrefWidth(300)

        def pythonBtn = new Button("Browse...")
        pythonBtn.setOnAction {
            def chooser = new FileChooser()
            chooser.setTitle("Select Python Executable")
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Python", "python.exe", "python")
            )
            def file = chooser.showOpenDialog(dialog.getOwner())
            if (file != null) {
                pythonField.setText(file.getAbsolutePath())
            }
        }

        def modelsField = new TextField(SPIDERConfig.modelsBasePath)
        modelsField.setPrefWidth(300)
        def modelsBtn = new Button("Browse...")
        modelsBtn.setOnAction {
            def chooser = new DirectoryChooser()
            chooser.setTitle("Select Models Directory")
            def dir = chooser.showDialog(dialog.getOwner())
            if (dir != null) {
                modelsField.setText(dir.getAbsolutePath())
            }
        }

        grid.add(new Label("Python Path:"), 0, 0)
        grid.add(pythonField, 1, 0)
        grid.add(pythonBtn, 2, 0)

        grid.add(new Label("Models Directory:"), 0, 1)
        grid.add(modelsField, 1, 1)
        grid.add(modelsBtn, 2, 1)

        def testBtn = new Button("Test Configuration")
        testBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;")
        testBtn.setOnAction {
            testConfiguration(pythonField.getText(), modelsField.getText())
        }
        grid.add(testBtn, 1, 2)

        dialog.getDialogPane().setContent(grid)
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        def result = dialog.showAndWait()
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SPIDERConfig.pythonPath = pythonField.getText()
            SPIDERConfig.modelsBasePath = modelsField.getText()
            SPIDERConfig.save()
            Dialogs.showInfoNotification("Configuration Saved", "Settings have been saved successfully")
        }
    }
}

def testConfiguration(pythonPath, modelsPath) {
    def pythonOk = new File(pythonPath).exists()
    def colorectalOk = new File(modelsPath, "SPIDER-colorectal-model").exists()
    def skinOk = new File(modelsPath, "SPIDER-skin-model").exists()
    def thoraxOk = new File(modelsPath, "SPIDER-thorax-model").exists()

    def message = "Configuration Test Results:\n\n"
    message += "Python: ${pythonOk ? '✓ Found' : '✗ Not Found'}\n"
    message += "Colorectal Model: ${colorectalOk ? '✓ Found' : '✗ Not Found'}\n"
    message += "Skin Model: ${skinOk ? '✓ Found' : '✗ Not Found'}\n"
    message += "Thorax Model: ${thoraxOk ? '✓ Found' : '✗ Not Found'}\n"

    if (pythonOk && (colorectalOk || skinOk || thoraxOk)) {
        Dialogs.showInfoNotification("Configuration Test", message)
    } else {
        Dialogs.showErrorMessage("Configuration Test", message)
    }
}

def showTutorial() {
    Platform.runLater {
        def dialog = new Dialog<ButtonType>()
        dialog.setTitle("SPIDER Tutorial")
        dialog.setHeaderText("How to Use SPIDER Models")
        def content = new VBox(10)
        content.setPadding(new Insets(10))
        def tutorialText = """
SPIDER Workflow Tutorial:

STEP 1: Initial Setup
• Install Python and required packages
• Download SPIDER models from Hugging Face
• Configure paths in the Configuration menu

STEP 2: Prepare Your Slide
• Open your whole slide image (.svs) in QuPath
• Navigate to regions of interest
• Use annotation tools to mark areas

STEP 3: Classification Methods

Method A - Classify Annotations:
• Draw annotations around specific regions
• Select annotations to classify
• Run \"Classify Selected Annotations\"
• Best for: Quick classification of specific areas

Method B - Tile Analysis:
• Draw larger annotation covering heterogeneous tissue
• Run \"Tile Analysis\" 
• Creates 1120×1120 pixel grid
• Best for: Detailed spatial analysis

Method C - Whole Slide Analysis:
• No annotations needed
• Processes entire slide
• Generates heatmaps for each class
• Best for: Overview of tissue distribution

STEP 4: Interpret Results
• Classifications shown as colors
• Confidence scores in measurements
• Tile analysis shows tissue composition percentages

Tips:
• Start with small regions to test
• Use appropriate model for tissue type
• Check confidence scores for reliability
"""
        def textArea = new TextArea(tutorialText)
        textArea.setEditable(false)
        textArea.setWrapText(true)
        textArea.setPrefSize(500, 500)
        content.getChildren().add(textArea)
        dialog.getDialogPane().setContent(content)
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK)
        dialog.setResizable(true)
        dialog.showAndWait()
    }
}

def runClassifyAnnotations(modelType) {
    println("Running classification with ${modelType} model")
    if (!SPIDERConfig.pythonPath || !SPIDERConfig.modelsBasePath) {
        Dialogs.showErrorMessage("Configuration Required", 
            "Please configure Python path and models directory first")
        createMainMenu()
        return
    }
    def modelInfo = getModelInfo()
    def modelPath = Paths.get(SPIDERConfig.modelsBasePath, modelInfo[modelType].fullName).toString()
    if (!new File(modelPath).exists()) {
        Dialogs.showErrorMessage("Model not found at: ${modelPath}\n\nPlease download from Hugging Face")
        return
    }
    def projectPath = QPEx.PROJECT_BASE_DIR
    def pythonScriptPath = new File(QPEx.buildFilePath(projectPath, "python/spider_qupath_classifier.py"))
    if (!pythonScriptPath.exists()) {
        Dialogs.showErrorMessage("Python Script Missing\n\nspider_qupath_classifier.py not found in project directory.\n\nPlease ensure all Python scripts are in your project folder.")
        return
    }
    def groovyScript = new File(QPEx.buildFilePath(projectPath, "scripts/spider_classify_annotations.groovy"))
    if (groovyScript.exists()) {
        def scriptText = groovyScript.text
        scriptText = scriptText.replaceAll(/def pythonPath = ".*"/, "def pythonPath = \"${SPIDERConfig.pythonPath}\"")
        scriptText = scriptText.replaceAll(/def modelPath = ".*"/, "def modelPath = \"${modelPath}\"")
        def tempScript = File.createTempFile("spider_classify_temp_", ".groovy")
        tempScript.text = scriptText
        runScript(tempScript.getAbsolutePath())
        tempScript.delete()
    } else {
        Dialogs.showErrorMessage("Script Missing\n\nspider_classify_annotations.groovy not found in project directory.")
    }
}

def runTileAnalysis(modelType) {
    println("Running tile analysis with ${modelType} model")
    if (!SPIDERConfig.pythonPath || !SPIDERConfig.modelsBasePath) {
        Dialogs.showErrorMessage("Configuration Required", 
            "Please configure Python path and models directory first")
        createMainMenu()
        return
    }
    def modelInfo = getModelInfo()
    def modelPath = Paths.get(SPIDERConfig.modelsBasePath, modelInfo[modelType].fullName).toString()
    if (!new File(modelPath).exists()) {
        Dialogs.showErrorMessage("Model not found at: ${modelPath}\n\nPlease download from Hugging Face")
        return
    }
    def projectPath = QPEx.PROJECT_BASE_DIR
    def pythonScriptPath = new File(QPEx.buildFilePath(projectPath, "python/spider_qupath_classifier.py"))
    if (!pythonScriptPath.exists()) {
        Dialogs.showErrorMessage("Python Script Missing\n\nspider_qupath_classifier.py not found in project directory.\n\nPlease ensure all Python scripts are in your project folder.")
        return
    }
    def groovyScript = new File(QPEx.buildFilePath(projectPath, "scripts/spider_tile_classifier.groovy"))
    if (groovyScript.exists()) {
        def scriptText = groovyScript.text
        scriptText = scriptText.replaceAll(/def pythonPath = ".*"/, "def pythonPath = \"${SPIDERConfig.pythonPath}\"")
        scriptText = scriptText.replaceAll(/def modelPath = ".*"/, "def modelPath = \"${modelPath}\"")
        if (modelType != "colorectal") {
            def newAbbrev = getClassAbbreviations(modelType)
            def abbrevString = newAbbrev.collect { k, v -> 
                "    \"${k}\": \"${v}\""
            }.join(",\n")
            scriptText = scriptText.replaceAll(/def classAbbreviations = \[[\s\S]*?\]/, 
                "def classAbbreviations = [\n${abbrevString}\n]")
        }
        def tempScript = File.createTempFile("spider_tile_temp_", ".groovy")
        tempScript.text = scriptText
        runScript(tempScript.getAbsolutePath())
        tempScript.delete()
    } else {
        Dialogs.showErrorMessage("Script Missing\n\nspider_tile_classifier.groovy not found in project directory.")
    }
}

def showWholeSlideDialog(modelType) {
    Platform.runLater {
        def dialog = new Dialog<ButtonType>()
        dialog.setTitle("Whole Slide Analysis")
        dialog.setHeaderText("Configure Whole Slide Analysis")
        def grid = new GridPane()
        grid.setHgap(10)
        grid.setVgap(10)
        grid.setPadding(new Insets(20))

        def outputField = new TextField()
        outputField.setPrefWidth(300)
        def outputBtn = new Button("Browse...")
        outputBtn.setOnAction {
            def chooser = new DirectoryChooser()
            chooser.setTitle("Select Output Directory")
            def dir = chooser.showDialog(dialog.getOwner())
            if (dir != null) {
                outputField.setText(dir.getAbsolutePath())
            }
        }

        def strideField = new TextField("560")
        def maxPatchesField = new TextField("1000")
        def workersField = new TextField("4")

        grid.add(new Label("Output Directory:"), 0, 0)
        grid.add(outputField, 1, 0)
        grid.add(outputBtn, 2, 0)

        grid.add(new Label("Patch Stride:"), 0, 1)
        grid.add(strideField, 1, 1)
        grid.add(new Label("(560 = 50% overlap)"), 2, 1)

        grid.add(new Label("Max Patches:"), 0, 2)
        grid.add(maxPatchesField, 1, 2)
        grid.add(new Label("(limit for memory)"), 2, 2)

        grid.add(new Label("Workers:"), 0, 3)
        grid.add(workersField, 1, 3)
        grid.add(new Label("(parallel processing)"), 2, 3)

        dialog.getDialogPane().setContent(grid)
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        def result = dialog.showAndWait()
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runWholeSlideAnalysis(modelType, outputField.getText(), 
                strideField.getText(), maxPatchesField.getText(), workersField.getText())
        }
    }
}

def runWholeSlideAnalysis(modelType, outputDir, stride, maxPatches, workers) {
    def modelInfo = getModelInfo()
    def modelPath = Paths.get(SPIDERConfig.modelsBasePath, modelInfo[modelType].fullName).toString()
    def slidePath = getCurrentImageData().getServer().getPath()
    if (slidePath.startsWith("file:")) {
        slidePath = slidePath.substring(5)
    }
    def projectPath = QPEx.PROJECT_BASE_DIR
    def scriptPath = new File(QPEx.buildFilePath(projectPath, "python/whole_slide_analysis_spider_colorectal.py"))
    if (!scriptPath.exists()) {
        Dialogs.showErrorMessage("Script Missing\n\nwhole_slide_analysis_spider_colorectal.py not found in project directory.\n\nThis script is needed for whole slide analysis.")
        return
    }
    def command = [
        SPIDERConfig.pythonPath,
        scriptPath.getAbsolutePath(),
        modelPath,
        slidePath,
        outputDir,
        stride,
        maxPatches,
        workers
    ]
    println("Running: " + command.join(" "))
    def process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()
    Dialogs.showInfoNotification("Whole Slide Analysis Started", 
        "Processing slide... Results will be saved to:\n${outputDir}")
}

def getClassAbbreviations(modelType) {
    def abbrevMap = [:]
    if (modelType == "colorectal") {
        abbrevMap = [
            "Adenocarcinoma high grade": "Adeno H",
            "Adenocarcinoma low grade": "Adeno L",
            "Adenoma high grade": "Aden H",
            "Adenoma low grade": "Aden L",
            "Fat": "Fat",
            "Hyperplastic polyp": "Hyper",
            "Inflammation": "Infl",
            "Mucus": "Mucus",
            "Muscle": "Musc",
            "Necrosis": "Necr",
            "Sessile serrated lesion": "SSL",
            "Stroma healthy": "Stroma",
            "Vessels": "Vessel"
        ]
    } else if (modelType == "skin") {
        abbrevMap = [
            "Basal Cell Carcinoma": "BCC",
            "Melanoma invasive": "Mel Inv",
            "Melanoma in situ": "Mel IS",
            "Squamous Cell Carcinoma": "SCC",
            "Epidermis": "Epid",
            "Dermis": "Derm",
            "Vessels": "Vessel"
        ]
    } else if (modelType == "thorax") {
        abbrevMap = [
            "Small cell carcinoma": "Small C",
            "Non-small cell carcinoma": "NSCLC",
            "Alveoli": "Alv",
            "Vessels": "Vessel",
            "Bronchus": "Bronch"
        ]
    }
    return abbrevMap
}

try {
    def gui = QPEx.getQuPath()
    def menu = gui.getMenu("Extensions>SPIDER Analysis", true)
    menu.getItems().clear()
    def menuItem = new MenuItem("Open SPIDER Menu")
    menuItem.setOnAction { createMainMenu() }
    menu.getItems().add(menuItem)
    menuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+S"))
    println("SPIDER Plugin installed! Access via Extensions > SPIDER Analysis or press Ctrl+Shift+S")
} catch (Exception e) {
    println("Could not install menu item: " + e.getMessage())
    println("You can still run the script manually to open the SPIDER menu.")
}

createMainMenu()

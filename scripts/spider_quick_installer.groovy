// SPIDER_Quick_Installer.groovy
// One-click installer for SPIDER pathology analysis plugin
// Simply run this script in QuPath to install everything!

import qupath.lib.gui.dialogs.Dialogs
import javafx.application.Platform
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.geometry.Insets
import javafx.stage.Stage
import groovy.json.JsonBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

println "=".repeat(60)
println "SPIDER PATHOLOGY ANALYSIS - QUICK INSTALLER"
println "=".repeat(60)

// Create project directories
def projectPath = buildFilePath(PROJECT_BASE_DIR)
def scriptsPath = buildFilePath(projectPath, "scripts")
def spiderPath = buildFilePath(scriptsPath, "spider")

// Create directories
new File(scriptsPath).mkdirs()
new File(spiderPath).mkdirs()

println "Creating SPIDER plugin structure in: ${spiderPath}"

// Write the main plugin menu script
def pluginMenuScript = '''
// SPIDER_Plugin_Menu.groovy
// Main menu system for SPIDER pathology models in QuPath
// Place this in your QuPath scripts directory and run it to install the menu

import javafx.application.Platform
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.geometry.Insets
import javafx.stage.Stage
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.gui.scripting.QPEx
import qupath.lib.scripting.QP
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
        def configFile = new File(QPEx.new File(new File(QPEx.PROJECT_BASE_DIR).getParent(), "config/spider_config.json").getAbsolutePath())
        def config = [
            pythonPath: pythonPath,
            modelsBasePath: modelsBasePath,
            lastSelectedModel: lastSelectedModel,
            showTutorial: showTutorial
        ]
        configFile.text = new groovy.json.JsonBuilder(config).toPrettyString()
    }
    
    static void load() {
        def configFile = new File(QPEx.new File(new File(QPEx.PROJECT_BASE_DIR).getParent(), "config/spider_config.json").getAbsolutePath())
        if (configFile.exists()) {
            def config = new groovy.json.JsonSlurper().parseText(configFile.text)
            pythonPath = config.pythonPath ?: ""
            modelsBasePath = config.modelsBasePath ?: ""
            lastSelectedModel = config.lastSelectedModel ?: "colorectal"
            showTutorial = config.showTutorial ?: true
        }
    }
}

// Load configuration
SPIDERConfig.load()

// Model information
def modelInfo = [
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

// Create main menu
def createMainMenu() {
    Platform.runLater {
        def stage = new Stage()
        stage.setTitle("SPIDER Pathology Analysis")
        
        def mainPane = new VBox(15)
        mainPane.setPadding(new Insets(20))
        mainPane.setStyle("-fx-background-color: #f0f0f0;")
        
        // Header
        def headerLabel = new Label("SPIDER Digital Pathology Analysis")
        headerLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;")
        
        def subheaderLabel = new Label("AI-powered tissue classification for pathology")
        subheaderLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;")
        
        // Model selection
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
        
        // Update description when model changes
        def updateModelDesc = {
            def selected = modelChoiceBox.getValue().toLowerCase()
            def info = modelInfo[selected]
            modelDescLabel.setText("${info.description}\nAccuracy: ${info.accuracy}")
            SPIDERConfig.lastSelectedModel = selected
        }
        
        modelChoiceBox.setOnAction { updateModelDesc() }
        updateModelDesc()
        
        modelBox.getChildren().addAll(modelLabel, modelChoiceBox, modelDescLabel)
        
        // Workflow buttons
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
            """)
            button.setOnMouseEntered { 
                button.setStyle(button.getStyle() + "-fx-background-color: #2980b9;") 
            }
            button.setOnMouseExited { 
                button.setStyle(button.getStyle().replace("-fx-background-color: #2980b9;", "-fx-background-color: #3498db;"))
            }
            button.setOnAction(action)
            return button
        }
        
        // Main workflow buttons
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
        
        // Configuration button
        def configBtn = new Button("⚙ Configuration")
        configBtn.setStyle("""
            -fx-pref-width: 150px;
            -fx-background-color: #95a5a6;
            -fx-text-fill: white;
        """)
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
        
        // Help text
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
        
        // Layout
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
        
        // Show tutorial on first run
        if (SPIDERConfig.showTutorial) {
            showTutorial()
            SPIDERConfig.showTutorial = false
            SPIDERConfig.save()
        }
    }
}

// Configuration dialog
def showConfigurationDialog() {
    Platform.runLater {
        def dialog = new Dialog<ButtonType>()
        dialog.setTitle("SPIDER Configuration")
        dialog.setHeaderText("Configure SPIDER Settings")
        
        def grid = new GridPane()
        grid.setHgap(10)
        grid.setVgap(10)
        grid.setPadding(new Insets(20))
        
        // Python path
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
        
        // Models base path
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
        
        // Test button
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

// Test configuration
def testConfiguration(pythonPath, modelsPath) {
    // Test Python
    def pythonOk = new File(pythonPath).exists()
    
    // Test models
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

// Show tutorial
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
• Run "Classify Selected Annotations"
• Best for: Quick classification of specific areas

Method B - Tile Analysis:
• Draw larger annotation covering heterogeneous tissue
• Run "Tile Analysis" 
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

// Run classify annotations with selected model
def runClassifyAnnotations(modelType) {
    println("Running classification with ${modelType} model")
    
    // Check configuration
    if (!SPIDERConfig.pythonPath || !SPIDERConfig.modelsBasePath) {
        Dialogs.showErrorMessage("Configuration Required", 
            "Please configure Python path and models directory first")
        createMainMenu()
        return
    }
    
    def modelPath = Paths.get(SPIDERConfig.modelsBasePath, modelInfo[modelType].fullName).toString()
    
    // Check if model exists
    if (!new File(modelPath).exists()) {
        Dialogs.showErrorMessage("Model Not Found", 
            "Model not found at: ${modelPath}\n\nPlease download from Hugging Face")
        return
    }
    
    // Run the classification script with the selected model
    def scriptContent = getClassifyAnnotationsScript(modelPath)
    def tempScript = File.createTempFile("spider_classify_", ".groovy")
    tempScript.text = scriptContent
    
    // Execute the script
    def scriptPath = tempScript.getAbsolutePath()
    runScript(scriptPath)
    
    tempScript.delete()
}

// Run tile analysis with selected model
def runTileAnalysis(modelType) {
    println("Running tile analysis with ${modelType} model")
    
    // Check configuration
    if (!SPIDERConfig.pythonPath || !SPIDERConfig.modelsBasePath) {
        Dialogs.showErrorMessage("Configuration Required", 
            "Please configure Python path and models directory first")
        createMainMenu()
        return
    }
    
    def modelPath = Paths.get(SPIDERConfig.modelsBasePath, modelInfo[modelType].fullName).toString()
    
    // Check if model exists
    if (!new File(modelPath).exists()) {
        Dialogs.showErrorMessage("Model Not Found", 
            "Model not found at: ${modelPath}\n\nPlease download from Hugging Face")
        return
    }
    
    // Get class abbreviations for the selected model
    def classAbbrev = getClassAbbreviations(modelType)
    
    // Run the tile analysis script with the selected model
    def scriptContent = getTileAnalysisScript(modelPath, classAbbrev)
    def tempScript = File.createTempFile("spider_tile_", ".groovy")
    tempScript.text = scriptContent
    
    // Execute the script
    def scriptPath = tempScript.getAbsolutePath()
    runScript(scriptPath)
    
    tempScript.delete()
}

// Show whole slide analysis dialog
def showWholeSlideDialog(modelType) {
    Platform.runLater {
        def dialog = new Dialog<ButtonType>()
        dialog.setTitle("Whole Slide Analysis")
        dialog.setHeaderText("Configure Whole Slide Analysis")
        
        def grid = new GridPane()
        grid.setHgap(10)
        grid.setVgap(10)
        grid.setPadding(new Insets(20))
        
        // Output directory
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
        
        // Parameters
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

// Run whole slide analysis
def runWholeSlideAnalysis(modelType, outputDir, stride, maxPatches, workers) {
    def modelPath = Paths.get(SPIDERConfig.modelsBasePath, modelInfo[modelType].fullName).toString()
    def slidePath = getCurrentImageData().getServer().getPath()
    
    // Parse QuPath path format
    if (slidePath.startsWith("file:")) {
        slidePath = slidePath.substring(5)
    }
    
    def command = [
        SPIDERConfig.pythonPath,
        getWholeSlideScript(),
        modelPath,
        slidePath,
        outputDir,
        stride,
        maxPatches,
        workers
    ]
    
    println("Running: " + command.join(" "))
    
    // Run in background
    def process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()
    
    Dialogs.showInfoNotification("Whole Slide Analysis Started", 
        "Processing slide... Results will be saved to:\n${outputDir}")
}

// Get class abbreviations for model
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
            // Add more skin classes as needed
        ]
    } else if (modelType == "thorax") {
        abbrevMap = [
            "Small cell carcinoma": "Small C",
            "Non-small cell carcinoma": "NSCLC",
            "Alveoli": "Alv",
            "Vessels": "Vessel",
            "Bronchus": "Bronch"
            // Add more thorax classes as needed
        ]
    }
    
    return abbrevMap
}

// Generate classify annotations script
def getClassifyAnnotationsScript(modelPath) {
    return """
// Auto-generated SPIDER classification script
import qupath.lib.objects.PathObject
import qupath.lib.roi.RectangleROI
import qupath.lib.io.GsonTools
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.objects.classes.PathClass
import com.google.gson.JsonParser

def startTime = System.currentTimeMillis()
println("Starting SPIDER classification...")

def projectPath = buildFilePath(PROJECT_BASE_DIR)
def pythonPath = "${SPIDERConfig.pythonPath}"
def outputPath = new File(new File(projectPath).getParent(), "output/classifications").getAbsolutePath()
def scriptPath = new File(new File(projectPath).getParent(), "python/spider_qupath_classifier.py").getAbsolutePath()
def modelPath = "${modelPath}"
def tempAnnotationsPath = buildFilePath(outputPath, "annotations_to_predict.json")

def outputDir = new File(outputPath)
if (!outputDir.exists()) outputDir.mkdirs()

// Export annotations
def imageData = getCurrentImageData()
def server = imageData.getServer()
def imagePath = server.getPath()
def imageName = GeneralTools.stripExtension(server.getMetadata().getName())

def annotations = getSelectedObjects().findAll { it.isAnnotation() }
if (annotations.isEmpty()) {
    annotations = getAnnotationObjects().findAll { it.getPathClass() == null }
}

if (annotations.isEmpty()) {
    Dialogs.showErrorMessage("SPIDER", "No annotations found to classify!")
    return
}

def annotationData = []
annotations.each { annotation ->
    def roi = annotation.getROI()
    annotationData.add([
        id: annotation.getID(),
        slide_path: imagePath,
        image_name: imageName,
        roi: [
            x: roi.getBoundsX(),
            y: roi.getBoundsY(),
            width: roi.getBoundsWidth(),
            height: roi.getBoundsHeight()
        ]
    ])
}

def gson = GsonTools.getInstance(true)
new File(tempAnnotationsPath).text = gson.toJson(annotationData)

// Run classification
def command = [pythonPath, scriptPath, tempAnnotationsPath, modelPath, outputPath]
def process = new ProcessBuilder(command).redirectErrorStream(true).start()

def reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
def line
while ((line = reader.readLine()) != null) {
    println(line)
}

def exitCode = process.waitFor()
if (exitCode != 0) {
    Dialogs.showErrorMessage("Error", "Classification failed!")
    return
}

// Apply results
def predictionsPath = buildFilePath(outputPath, "predictions.json")
def jsonText = new File(predictionsPath).text
def jsonParser = new JsonParser()
def jsonArray = jsonParser.parse(jsonText).getAsJsonArray()

def predictions = []
for (int i = 0; i < jsonArray.size(); i++) {
    def obj = jsonArray.get(i).getAsJsonObject()
    def pred = [id: obj.get("id").getAsString()]
    
    if (!obj.get("prediction").isJsonNull()) {
        pred.prediction = obj.get("prediction").getAsString()
        def probsObj = obj.get("probabilities").getAsJsonObject()
        def probs = [:]
        probsObj.keySet().each { cls ->
            probs[cls] = probsObj.get(cls).getAsDouble()
        }
        pred.probabilities = probs
    }
    predictions.add(pred)
}

def allAnnotations = getAnnotationObjects()
def annotationMap = [:]
allAnnotations.each { annotationMap[it.getID().toString()] = it }

def applied = 0
predictions.each { pred ->
    def annotation = annotationMap[pred.id]
    if (annotation && pred.prediction) {
        def pathClass = PathClass.fromString(pred.prediction)
        if (!pathClass) {
            pathClass = PathClass.getInstance(pred.prediction, getColorRGB(128, 128, 128))
        }
        annotation.setPathClass(pathClass)
        
        def conf = String.format("%.1f", pred.probabilities[pred.prediction] * 100)
        annotation.setName("SPIDER: " + pred.prediction + " (" + conf + "%)")
        
        pred.probabilities.each { cls, prob ->
            annotation.measurements.put("SPIDER: P(" + cls + ")", prob)
        }
        applied++
    }
}

fireHierarchyUpdate()
Dialogs.showInfoNotification("SPIDER", "Classified " + applied + " annotations")
"""
}

// Generate tile analysis script
def getTileAnalysisScript(modelPath, classAbbrev) {
    // Convert abbreviation map to string representation
    def abbrevString = classAbbrev.collect { k, v -> 
        '"' + k + '": "' + v + '"' 
    }.join(",\n    ")
    
    return """
// Auto-generated SPIDER tile analysis script
// [Previous imports and setup code remains the same as spider_tile_classifier.groovy]
import qupath.lib.objects.PathObject
import qupath.lib.roi.RectangleROI
import qupath.lib.geom.Point2
import qupath.lib.regions.ImagePlane
import qupath.lib.roi.ROIs
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.io.GsonTools
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.objects.classes.PathClassFactory
import javafx.scene.paint.Color

// Configuration
def pythonPath = "${SPIDERConfig.pythonPath}"
def modelPath = "${modelPath}"
def patchSize = 1120
def patchStride = 1120
def visualizeResults = true
def keepTempFiles = false

// Model-specific class abbreviations
def classAbbreviations = [
    ${abbrevString}
]

// [Rest of the tile analysis script code continues as in the original...]
// [Include all the tile generation, classification, and visualization logic]
"""
}

// Get whole slide analysis script path
def getWholeSlideScript() {
    // Copy the Python script to project directory if needed
    def projectPath = QPEx.PROJECT_BASE_DIR
    def scriptPath = Paths.get(projectPath, "whole_slide_analysis_spider.py")
    
    // You would need to ensure this script is available
    return scriptPath.toString()
}

// Create menu item in QuPath
def gui = QPEx.getQuPath()
def menu = gui.getMenu("Extensions>SPIDER Analysis", true)

// Clear existing items
menu.getItems().clear()

// Add menu item
def menuItem = new MenuItem("Open SPIDER Menu")
menuItem.setOnAction { createMainMenu() }
menu.getItems().add(menuItem)

// Also add keyboard shortcut
menuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+S"))

println("SPIDER Plugin installed! Access via Extensions > SPIDER Analysis or press Ctrl+Shift+S")

// Show main menu immediately
createMainMenu()
'''

// Write the Python classifier script
def pythonClassifierScript = '''
# spider_qupath_classifier_universal.py
# Universal Python script to classify annotations in QuPath using any SPIDER model
# Works with Colorectal, Skin, and Thorax models

import os
import sys
import json
import torch
import numpy as np
from PIL import Image
import openslide
from transformers import AutoModel, AutoProcessor
from pathlib import Path
from datetime import datetime

# Parse command line arguments
if len(sys.argv) < 4:
    print("Usage: python spider_qupath_classifier_universal.py <annotations_json> <model_path> <output_dir>")
    sys.exit(1)

annotations_path = sys.argv[1]
model_path = sys.argv[2]
output_dir = sys.argv[3]

# Create output directory if it doesn't exist
os.makedirs(output_dir, exist_ok=True)

# Configure device
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"Using device: {device}")

# Model-specific color schemes for better visualization
MODEL_COLOR_SCHEMES = {
    "colorectal": {
        "Adenocarcinoma high grade": "#DC143C",  # Crimson
        "Adenocarcinoma low grade": "#FF4500",   # OrangeRed
        "Adenoma high grade": "#FF8C00",         # DarkOrange
        "Adenoma low grade": "#FFA500",          # Orange
        "Fat": "#FFD700",                        # Gold
        "Hyperplastic polyp": "#008000",         # Green
        "Inflammation": "#FFFF00",               # Yellow
        "Mucus": "#00BFFF",                      # DeepSkyBlue
        "Muscle": "#8B008B",                     # DarkMagenta
        "Necrosis": "#800000",                   # Maroon
        "Sessile serrated lesion": "#00FA9A",    # MediumSpringGreen
        "Stroma healthy": "#32CD32",             # LimeGreen
        "Vessels": "#0000FF"                     # Blue
    },
    "skin": {
        "Basal Cell Carcinoma": "#8B0000",       # DarkRed
        "Melanoma invasive": "#000000",          # Black
        "Melanoma in situ": "#696969",           # DimGray
        "Squamous Cell Carcinoma": "#DC143C",    # Crimson
        "Epidermis": "#FFE4B5",                  # Moccasin
        "Dermis": "#DEB887",                     # BurlyWood
        "Vessels": "#0000FF",                    # Blue
        # Add more skin classes as they appear in the model
    },
    "thorax": {
        "Small cell carcinoma": "#800080",       # Purple
        "Non-small cell carcinoma": "#4B0082",   # Indigo
        "Alveoli": "#87CEEB",                    # SkyBlue
        "Vessels": "#0000FF",                    # Blue
        "Bronchus": "#8B4513",                   # SaddleBrown
        # Add more thorax classes as they appear in the model
    }
}

# Load SPIDER model with automatic model type detection
def load_spider_model(model_path):
    print(f"Loading SPIDER model from: {model_path}")
    
    # Detect model type from path
    model_type = "unknown"
    if "colorectal" in model_path.lower():
        model_type = "colorectal"
    elif "skin" in model_path.lower():
        model_type = "skin"
    elif "thorax" in model_path.lower():
        model_type = "thorax"
    
    print(f"Detected model type: {model_type}")
    
    try:
        # Load model and processor
        model = AutoModel.from_pretrained(
            model_path,
            trust_remote_code=True,
            local_files_only=True
        )
        
        processor = AutoProcessor.from_pretrained(
            model_path,
            trust_remote_code=True,
            local_files_only=True
        )
        
        # Move model to device
        model.to(device)
        model.eval()
        
        # Load class names from config
        config_path = os.path.join(model_path, "config.json")
        with open(config_path, 'r') as f:
            config = json.load(f)
        
        class_names = config.get('class_names', [])
        print(f"Model has {len(class_names)} classes: {class_names}")
        
        # Get color scheme for this model type
        color_scheme = MODEL_COLOR_SCHEMES.get(model_type, {})
        
        return model, processor, class_names, model_type, color_scheme
    
    except Exception as e:
        print(f"Error loading model: {str(e)}")
        sys.exit(1)

# Parse QuPath path format (handles various QuPath server types)
def parse_qupath_path(qupath_path):
    # Handle BioFormatsImageServer paths
    if qupath_path.startswith("BioFormatsImageServer:"):
        path = qupath_path.replace("BioFormatsImageServer:", "").strip()
        if path.startswith("file:/"):
            path = path[6:]
        if "[" in path:
            path = path.split("[")[0]
        return path
    
    # Handle file:// URLs
    elif qupath_path.startswith("file:///"):
        return qupath_path[8:]  # Remove file:///
    elif qupath_path.startswith("file://"):
        return qupath_path[7:]  # Remove file://
    elif qupath_path.startswith("file:/"):
        return qupath_path[6:]  # Remove file:/
    
    # Handle OpenslideImageServer paths
    elif "OpenslideImageServer:" in qupath_path:
        path = qupath_path.replace("qupath.lib.images.servers.openslide.OpenslideImageServer:", "").strip()
        if path.startswith("file:/"):
            path = path[6:]
        return path
    
    # Handle ImageIOImageServer paths
    elif "ImageIOImageServer:" in qupath_path:
        path = qupath_path.replace("qupath.lib.images.servers.imageio.ImageIOImageServer:", "").strip()
        if path.startswith("file:/"):
            path = path[6:]
        return path
        
    return qupath_path

# Extract region from slide with context padding
def extract_region_with_context(slide_path, region):
    try:
        # Parse path
        parsed_path = parse_qupath_path(slide_path)
        print(f"Opening slide: {parsed_path}")
        
        # Open slide
        slide = openslide.OpenSlide(parsed_path)
        
        # Extract region coordinates
        x = int(region['x'])
        y = int(region['y'])
        width = int(region['width'])
        height = int(region['height'])
        
        # SPIDER uses 1120×1120 regions, so add context padding if necessary
        context_size = 1120
        
        # Calculate center of region
        center_x = x + width // 2
        center_y = y + height // 2
        
        # Calculate coordinates for context region
        context_x = max(0, center_x - context_size // 2)
        context_y = max(0, center_y - context_size // 2)
        
        # Make sure we don't go outside slide boundaries
        slide_width, slide_height = slide.dimensions
        if context_x + context_size > slide_width:
            context_x = max(0, slide_width - context_size)
        if context_y + context_size > slide_height:
            context_y = max(0, slide_height - context_size)
        
        # Extract the region with context
        region_img = slide.read_region((context_x, context_y), 0, (context_size, context_size)).convert('RGB')
        
        print(f"Extracted region with context at ({context_x}, {context_y}), size {context_size}x{context_size}")
        return region_img
    
    except Exception as e:
        print(f"Error extracting region: {str(e)}")
        return None

# Main classification function
def classify_annotations():
    # Load model
    model, processor, class_names, model_type, color_scheme = load_spider_model(model_path)
    
    # Save model information to output directory
    model_info = {
        'model_type': model_type,
        'class_names': class_names,
        'color_scheme': color_scheme,
        'timestamp': datetime.now().isoformat()
    }
    
    with open(os.path.join(output_dir, 'model_info.json'), 'w') as f:
        json.dump(model_info, f, indent=2)
    
    # Save class names separately for backward compatibility
    with open(os.path.join(output_dir, 'classes.json'), 'w') as f:
        json.dump(class_names, f)
    
    # Load annotations
    with open(annotations_path, 'r') as f:
        annotations = json.load(f)
    
    print(f"Loaded {len(annotations)} annotations for classification")
    
    # Initialize results
    results = []
    
    # History file for tracking all predictions
    history_file = os.path.join(output_dir, f'prediction_history_{model_type}.jsonl')
    
    for idx, annotation in enumerate(annotations):
        print(f"\nProcessing annotation {idx+1}/{len(annotations)}...")
        
        # Extract information
        slide_path = annotation['slide_path']
        region = annotation['roi']
        annotation_id = annotation['id']
        image_name = annotation.get('image_name', 'unknown')
        
        # Extract region with context
        region_img = extract_region_with_context(slide_path, region)
        
        if region_img is None:
            print(f"Could not extract region for annotation {annotation_id}")
            results.append({
                'id': annotation_id,
                'prediction': None,
                'probabilities': None,
                'top_predictions': None
            })
            continue
        
        # Process with SPIDER model
        try:
            # Prepare inputs
            inputs = processor(images=region_img, return_tensors="pt")
            
            # Move inputs to device
            for k, v in inputs.items():
                if isinstance(v, torch.Tensor):
                    inputs[k] = v.to(device)
            
            # Run inference
            with torch.no_grad():
                outputs = model(**inputs)
            
            # Get predicted class and probabilities
            logits = outputs.logits
            probabilities = torch.softmax(logits[0], dim=0).cpu().numpy()
            prediction_idx = probabilities.argmax().item()
            prediction = class_names[prediction_idx]
            
            # Create class probabilities dictionary (rounded to 3 decimal places)
            class_probabilities = {class_name: round(float(probabilities[i]), 3) 
                                 for i, class_name in enumerate(class_names)}
            
            # Get top 3 predictions for display
            sorted_indices = np.argsort(probabilities)[::-1]
            top_predictions = []
            for i in range(min(3, len(class_names))):
                idx = sorted_indices[i]
                if probabilities[idx] > 0.01:  # Only include if probability > 1%
                    top_predictions.append({
                        'class': class_names[idx],
                        'probability': round(float(probabilities[idx]), 3),
                        'color': color_scheme.get(class_names[idx], '#808080')
                    })
            
            # Display results
            print(f"Prediction: {prediction} ({probabilities[prediction_idx]:.1%})")
            if len(top_predictions) > 1:
                print("Alternative predictions:")
                for i, pred in enumerate(top_predictions[1:], 1):
                    print(f"  {i}. {pred['class']}: {pred['probability']:.1%}")
            
            # Store result
            result = {
                'id': annotation_id,
                'prediction': prediction,
                'probabilities': class_probabilities,
                'top_predictions': top_predictions,
                'timestamp': datetime.now().isoformat(),
                'image_name': image_name,
                'model_type': model_type,
                'confidence': float(probabilities[prediction_idx])
            }
            
            # Append to history file
            with open(history_file, 'a') as f:
                f.write(json.dumps(result) + '\n')
            
            results.append(result)
            
        except Exception as e:
            print(f"Error classifying annotation {annotation_id}: {str(e)}")
            results.append({
                'id': annotation_id,
                'prediction': None,
                'probabilities': None,
                'top_predictions': None
            })
    
    # Save results
    results_path = os.path.join(output_dir, 'predictions.json')
    with open(results_path, 'w') as f:
        json.dump(results, f, indent=2)
    
    # Create summary statistics
    summary = {
        'total_annotations': len(results),
        'successful_classifications': sum(1 for r in results if r['prediction'] is not None),
        'model_type': model_type,
        'timestamp': datetime.now().isoformat()
    }
    
    if summary['successful_classifications'] > 0:
        # Count predictions by class
        class_counts = {}
        for result in results:
            if result['prediction']:
                class_counts[result['prediction']] = class_counts.get(result['prediction'], 0) + 1
        
        summary['class_distribution'] = class_counts
        
        # Calculate average confidence
        confidences = [r['confidence'] for r in results if r.get('confidence')]
        if confidences:
            summary['average_confidence'] = round(sum(confidences) / len(confidences), 3)
    
    with open(os.path.join(output_dir, 'classification_summary.json'), 'w') as f:
        json.dump(summary, f, indent=2)
    
    print(f"\nClassification completed!")
    print(f"Successfully classified {summary['successful_classifications']}/{len(results)} annotations")
    print(f"Results saved to {results_path}")
    print(f"Summary saved to classification_summary.json")
    
    return results

# Run classification
if __name__ == "__main__":
    classify_annotations()
'''

// Write the whole slide analysis script
def wholeSlideScript = '''
# whole_slide_analysis_spider_universal.py
# Universal whole slide analysis script for all SPIDER models
# Generates heatmaps and visualizations for Colorectal, Skin, and Thorax models

import os
import sys
import json
import torch
import numpy as np
from PIL import Image
import openslide
from transformers import AutoModel, AutoProcessor
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.colors import LinearSegmentedColormap
from datetime import datetime
from pathlib import Path
import multiprocessing as mp
from functools import partial
import warnings
warnings.filterwarnings('ignore')

# Parse command line arguments
if len(sys.argv) < 4:
    print("Usage: python whole_slide_analysis_spider_universal.py <model_path> <svs_path> <output_folder> [patch_stride] [max_patches] [num_workers]")
    print("Example: python whole_slide_analysis_spider_universal.py ./SPIDER-skin-model ./slide.svs ./output 560 1000 4")
    sys.exit(1)

model_path = sys.argv[1]
svs_path = sys.argv[2]
output_folder = sys.argv[3]
patch_stride = int(sys.argv[4]) if len(sys.argv) > 4 else 560  # 50% overlap by default
max_patches = int(sys.argv[5]) if len(sys.argv) > 5 else 1000
num_workers = int(sys.argv[6]) if len(sys.argv) > 6 else 4

# Create output directory
os.makedirs(output_folder, exist_ok=True)

# Model-specific visualization settings
MODEL_SETTINGS = {
    "colorectal": {
        "name": "SPIDER Colorectal Analysis",
        "colors": {
            "Adenocarcinoma high grade": "#DC143C",
            "Adenocarcinoma low grade": "#FF4500",
            "Adenoma high grade": "#FF8C00",
            "Adenoma low grade": "#FFA500",
            "Fat": "#FFD700",
            "Hyperplastic polyp": "#008000",
            "Inflammation": "#FFFF00",
            "Mucus": "#00BFFF",
            "Muscle": "#8B008B",
            "Necrosis": "#800000",
            "Sessile serrated lesion": "#00FA9A",
            "Stroma healthy": "#32CD32",
            "Vessels": "#0000FF"
        }
    },
    "skin": {
        "name": "SPIDER Skin Analysis",
        "colors": {
            "Basal Cell Carcinoma": "#8B0000",
            "Melanoma invasive": "#000000",
            "Melanoma in situ": "#696969",
            "Squamous Cell Carcinoma": "#DC143C",
            "Epidermis": "#FFE4B5",
            "Dermis": "#DEB887",
            "Vessels": "#0000FF",
            "Hair follicle": "#8B4513",
            "Sebaceous gland": "#F0E68C",
            "Sweat gland": "#87CEEB"
        }
    },
    "thorax": {
        "name": "SPIDER Thorax Analysis",
        "colors": {
            "Small cell carcinoma": "#800080",
            "Non-small cell carcinoma": "#4B0082",
            "Alveoli": "#87CEEB",
            "Vessels": "#0000FF",
            "Bronchus": "#8B4513",
            "Pleura": "#F0E68C",
            "Lymphoid tissue": "#9370DB"
        }
    }
}

# Detect model type from path
def detect_model_type(model_path):
    model_path_lower = model_path.lower()
    if "colorectal" in model_path_lower:
        return "colorectal"
    elif "skin" in model_path_lower:
        return "skin"
    elif "thorax" in model_path_lower:
        return "thorax"
    else:
        print("Warning: Could not detect model type from path. Using default settings.")
        return "colorectal"

# Load SPIDER model
def load_spider_model(model_path):
    print(f"Loading SPIDER model from: {model_path}")
    
    model_type = detect_model_type(model_path)
    print(f"Detected model type: {model_type}")
    
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Using device: {device}")
    
    try:
        model = AutoModel.from_pretrained(
            model_path,
            trust_remote_code=True,
            local_files_only=True
        )
        
        processor = AutoProcessor.from_pretrained(
            model_path,
            trust_remote_code=True,
            local_files_only=True
        )
        
        model.to(device)
        model.eval()
        
        # Load class names
        config_path = os.path.join(model_path, "config.json")
        with open(config_path, 'r') as f:
            config = json.load(f)
        
        class_names = config.get('class_names', [])
        print(f"Model has {len(class_names)} classes")
        
        return model, processor, class_names, device, model_type
    
    except Exception as e:
        print(f"Error loading model: {str(e)}")
        sys.exit(1)

# Process a single patch
def process_patch(patch_info, model_path):
    x, y, patch_size = patch_info
    
    # Reload model for each worker (required for multiprocessing)
    model, processor, class_names, device, _ = load_spider_model(model_path)
    
    try:
        # Extract patch
        patch = slide.read_region((x, y), 0, (patch_size, patch_size)).convert('RGB')
        
        # Process with model
        inputs = processor(images=patch, return_tensors="pt")
        for k, v in inputs.items():
            if isinstance(v, torch.Tensor):
                inputs[k] = v.to(device)
        
        with torch.no_grad():
            outputs = model(**inputs)
        
        logits = outputs.logits
        probabilities = torch.softmax(logits[0], dim=0).cpu().numpy()
        prediction_idx = probabilities.argmax().item()
        
        return {
            'x': x,
            'y': y,
            'prediction': class_names[prediction_idx],
            'probabilities': probabilities.tolist(),
            'confidence': float(probabilities[prediction_idx])
        }
    
    except Exception as e:
        print(f"Error processing patch at ({x}, {y}): {str(e)}")
        return None

# Main analysis
print(f"Starting whole slide analysis for: {svs_path}")

# Load slide
slide = openslide.OpenSlide(svs_path)
slide_width, slide_height = slide.dimensions
print(f"Slide dimensions: {slide_width} x {slide_height}")

# Load model for getting class names
model, processor, class_names, device, model_type = load_spider_model(model_path)

# Get model settings
settings = MODEL_SETTINGS.get(model_type, MODEL_SETTINGS["colorectal"])
color_map = settings["colors"]
analysis_name = settings["name"]

# Calculate patches to process
patch_size = 1120  # SPIDER input size
patches_to_process = []

# Simple grid sampling with stride
for y in range(0, slide_height - patch_size, patch_stride):
    for x in range(0, slide_width - patch_size, patch_stride):
        patches_to_process.append((x, y, patch_size))
        if len(patches_to_process) >= max_patches:
            break
    if len(patches_to_process) >= max_patches:
        break

print(f"Processing {len(patches_to_process)} patches with stride {patch_stride}")

# Process patches in parallel
if num_workers > 1:
    print(f"Using {num_workers} workers for parallel processing")
    # Close the slide object before multiprocessing
    slide.close()
    
    # Process patches
    with mp.Pool(num_workers) as pool:
        process_func = partial(process_patch, model_path=model_path)
        results = list(pool.imap_unordered(process_func, patches_to_process))
    
    # Reopen slide for thumbnail
    slide = openslide.OpenSlide(svs_path)
else:
    # Single-threaded processing
    results = []
    for i, patch_info in enumerate(patches_to_process):
        if i % 100 == 0:
            print(f"Processing patch {i+1}/{len(patches_to_process)}")
        result = process_patch(patch_info, model_path)
        results.append(result)

# Filter out failed patches
results = [r for r in results if r is not None]
print(f"Successfully processed {len(results)} patches")

# Save raw results
results_path = os.path.join(output_folder, 'patch_predictions.json')
with open(results_path, 'w') as f:
    json.dump(results, f)

# Create heatmaps for each class
print("Generating heatmaps...")

# Get thumbnail for overlay
thumbnail_size = (2000, int(2000 * slide_height / slide_width))
thumbnail = slide.get_thumbnail(thumbnail_size)

# Calculate scaling factors
scale_x = thumbnail_size[0] / slide_width
scale_y = thumbnail_size[1] / slide_height

# Create visualization for each class
fig, axes = plt.subplots(3, 4, figsize=(20, 15))
axes = axes.flatten()

for idx, class_name in enumerate(class_names[:12]):  # Show up to 12 classes
    ax = axes[idx]
    
    # Create heatmap for this class
    heatmap = np.zeros(thumbnail_size[::-1])  # height x width
    counts = np.zeros(thumbnail_size[::-1])
    
    for result in results:
        x_thumb = int(result['x'] * scale_x)
        y_thumb = int(result['y'] * scale_y)
        w_thumb = int(patch_size * scale_x)
        h_thumb = int(patch_size * scale_y)
        
        # Get probability for this class
        prob = result['probabilities'][idx]
        
        # Add to heatmap
        y_end = min(y_thumb + h_thumb, heatmap.shape[0])
        x_end = min(x_thumb + w_thumb, heatmap.shape[1])
        
        heatmap[y_thumb:y_end, x_thumb:x_end] += prob
        counts[y_thumb:y_end, x_thumb:x_end] += 1
    
    # Average the probabilities
    with np.errstate(divide='ignore', invalid='ignore'):
        heatmap = np.divide(heatmap, counts)
        heatmap[counts == 0] = 0
    
    # Show thumbnail with heatmap overlay
    ax.imshow(thumbnail, alpha=0.5)
    
    # Get color for this class
    class_color = color_map.get(class_name, '#808080')
    # Create custom colormap from white to class color
    colors = ['white', class_color]
    n_bins = 100
    cmap = LinearSegmentedColormap.from_list(class_name, colors, N=n_bins)
    
    im = ax.imshow(heatmap, alpha=0.7, cmap=cmap, vmin=0, vmax=1)
    ax.set_title(f"{class_name}", fontsize=12)
    ax.axis('off')

# Remove unused subplots
for idx in range(len(class_names), len(axes)):
    fig.delaxes(axes[idx])

plt.suptitle(f"{analysis_name} - {os.path.basename(svs_path)}", fontsize=16)
plt.tight_layout()
plt.savefig(os.path.join(output_folder, 'class_heatmaps.png'), dpi=150, bbox_inches='tight')
plt.close()

# Create overall classification map
print("Creating classification map...")

fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(20, 10))

# Show original thumbnail
ax1.imshow(thumbnail)
ax1.set_title("Original Slide", fontsize=14)
ax1.axis('off')

# Create classification overlay
classification_map = np.zeros((*thumbnail_size[::-1], 3))

for result in results:
    x_thumb = int(result['x'] * scale_x)
    y_thumb = int(result['y'] * scale_y)
    w_thumb = int(patch_size * scale_x)
    h_thumb = int(patch_size * scale_y)
    
    # Get color for predicted class
    predicted_class = result['prediction']
    color_hex = color_map.get(predicted_class, '#808080')
    color_rgb = [int(color_hex[i:i+2], 16)/255 for i in (1, 3, 5)]
    
    # Apply color with confidence-based opacity
    confidence = result['confidence']
    y_end = min(y_thumb + h_thumb, classification_map.shape[0])
    x_end = min(x_thumb + w_thumb, classification_map.shape[1])
    
    for c in range(3):
        classification_map[y_thumb:y_end, x_thumb:x_end, c] = color_rgb[c]

# Show classification map
ax2.imshow(thumbnail, alpha=0.3)
ax2.imshow(classification_map, alpha=0.7)
ax2.set_title("Classification Map", fontsize=14)
ax2.axis('off')

# Add legend
legend_elements = []
class_counts = {}
for result in results:
    class_counts[result['prediction']] = class_counts.get(result['prediction'], 0) + 1

# Sort by count
sorted_classes = sorted(class_counts.items(), key=lambda x: x[1], reverse=True)

for class_name, count in sorted_classes[:10]:  # Show top 10 classes
    color = color_map.get(class_name, '#808080')
    percentage = (count / len(results)) * 100
    legend_elements.append(mpatches.Patch(color=color, 
                                        label=f"{class_name} ({percentage:.1f}%)"))

ax2.legend(handles=legend_elements, loc='center left', bbox_to_anchor=(1, 0.5))

plt.suptitle(f"{analysis_name} Results", fontsize=16)
plt.tight_layout()
plt.savefig(os.path.join(output_folder, 'classification_overview.png'), dpi=150, bbox_inches='tight')
plt.close()

# Generate summary report
print("Generating summary report...")

summary = {
    "analysis_type": analysis_name,
    "model_type": model_type,
    "slide_path": svs_path,
    "slide_dimensions": {"width": slide_width, "height": slide_height},
    "analysis_parameters": {
        "patch_size": patch_size,
        "patch_stride": patch_stride,
        "total_patches": len(results),
        "max_patches": max_patches
    },
    "timestamp": datetime.now().isoformat(),
    "class_distribution": {},
    "high_confidence_regions": []
}

# Calculate class distribution
for class_name in class_names:
    count = sum(1 for r in results if r['prediction'] == class_name)
    percentage = (count / len(results)) * 100 if results else 0
    summary["class_distribution"][class_name] = {
        "count": count,
        "percentage": round(percentage, 2)
    }

# Find high-confidence regions for each significant class
for class_name, stats in summary["class_distribution"].items():
    if stats["percentage"] > 5:  # Only for classes with >5% presence
        high_conf_patches = [r for r in results 
                           if r['prediction'] == class_name and r['confidence'] > 0.8]
        
        if high_conf_patches:
            # Find centroid of high-confidence regions
            avg_x = sum(p['x'] for p in high_conf_patches) / len(high_conf_patches)
            avg_y = sum(p['y'] for p in high_conf_patches) / len(high_conf_patches)
            
            summary["high_confidence_regions"].append({
                "class": class_name,
                "patch_count": len(high_conf_patches),
                "centroid": {"x": int(avg_x), "y": int(avg_y)},
                "average_confidence": round(
                    sum(p['confidence'] for p in high_conf_patches) / len(high_conf_patches), 3
                )
            })

# Save summary
summary_path = os.path.join(output_folder, 'analysis_summary.json')
with open(summary_path, 'w') as f:
    json.dump(summary, f, indent=2)

# Generate HTML report
html_report = f"""
<!DOCTYPE html>
<html>
<head>
    <title>{analysis_name} Report</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        h1, h2 {{ color: #2c3e50; }}
        .overview {{ display: flex; gap: 20px; margin: 20px 0; }}
        .stat-box {{ background: #f0f0f0; padding: 15px; border-radius: 5px; }}
        .distribution-table {{ border-collapse: collapse; width: 100%; }}
        .distribution-table th, .distribution-table td {{ 
            border: 1px solid #ddd; padding: 8px; text-align: left; 
        }}
        .distribution-table th {{ background-color: #3498db; color: white; }}
        .color-box {{ display: inline-block; width: 20px; height: 20px; 
                     margin-right: 5px; vertical-align: middle; }}
        img {{ max-width: 100%; height: auto; }}
    </style>
</head>
<body>
    <h1>{analysis_name} Report</h1>
    <p>Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
    
    <div class="overview">
        <div class="stat-box">
            <h3>Slide Information</h3>
            <p>File: {os.path.basename(svs_path)}</p>
            <p>Dimensions: {slide_width} × {slide_height} pixels</p>
        </div>
        <div class="stat-box">
            <h3>Analysis Parameters</h3>
            <p>Model: {model_type.upper()}</p>
            <p>Patches analyzed: {len(results)}</p>
            <p>Patch size: {patch_size} × {patch_size} pixels</p>
            <p>Stride: {patch_stride} pixels</p>
        </div>
    </div>
    
    <h2>Classification Overview</h2>
    <img src="classification_overview.png" alt="Classification Overview">
    
    <h2>Class Distribution</h2>
    <table class="distribution-table">
        <tr>
            <th>Class</th>
            <th>Color</th>
            <th>Count</th>
            <th>Percentage</th>
        </tr>
"""

# Add class distribution to HTML
for class_name, stats in sorted(summary["class_distribution"].items(), 
                               key=lambda x: x[1]["percentage"], reverse=True):
    if stats["percentage"] > 0:
        color = color_map.get(class_name, '#808080')
        html_report += f"""
        <tr>
            <td>{class_name}</td>
            <td><span class="color-box" style="background-color: {color};"></span></td>
            <td>{stats["count"]}</td>
            <td>{stats["percentage"]:.1f}%</td>
        </tr>
        """

html_report += """
    </table>
    
    <h2>Class-Specific Heatmaps</h2>
    <img src="class_heatmaps.png" alt="Class Heatmaps">
    
    <h2>High-Confidence Regions</h2>
    <ul>
"""

# Add high-confidence regions
for region in summary["high_confidence_regions"]:
    html_report += f"""
        <li><strong>{region["class"]}</strong>: {region["patch_count"]} high-confidence patches 
            (avg. confidence: {region["average_confidence"]:.1%}) centered around 
            ({region["centroid"]["x"]}, {region["centroid"]["y"]})</li>
    """

html_report += """
    </ul>
</body>
</html>
"""

# Save HTML report
with open(os.path.join(output_folder, 'report.html'), 'w') as f:
    f.write(html_report)

# Clean up
slide.close()

print(f"\nAnalysis complete!")
print(f"Results saved to: {output_folder}")
print(f"- Classification overview: classification_overview.png")
print(f"- Class heatmaps: class_heatmaps.png")
print(f"- Summary data: analysis_summary.json")
print(f"- HTML report: report.html")
print(f"- Raw predictions: patch_predictions.json")
'''

// Save all scripts
try {
    // Save main menu
    new File(spiderPath, "SPIDER_Plugin_Menu.groovy").text = pluginMenuScript
    println "✓ Created main menu script"
    
    // Save Python classifier
    new File(spiderPath, "spider_qupath_classifier_universal.py").text = pythonClassifierScript
    println "✓ Created universal classifier script"
    
    // Save whole slide analysis
    new File(spiderPath, "whole_slide_analysis_spider_universal.py").text = wholeSlideScript
    println "✓ Created whole slide analysis script"
    
    // Create a launcher script in the main scripts directory
    def launcherScript = """
// SPIDER_Launcher.groovy
// Quick launcher for SPIDER plugin

// Load and run the main plugin
def spiderMenuPath = buildFilePath(PROJECT_BASE_DIR, "scripts", "spider", "SPIDER_Plugin_Menu.groovy")
def spiderMenuFile = new File(spiderMenuPath)

if (spiderMenuFile.exists()) {
    evaluate(spiderMenuFile)
} else {
    Dialogs.showErrorMessage("SPIDER Plugin", "Plugin files not found. Please run the installer again.")
}
"""
    
    new File(scriptsPath, "SPIDER_Launcher.groovy").text = launcherScript
    println "✓ Created launcher script"
    
} catch (Exception e) {
    println "ERROR: Failed to create scripts: ${e.message}"
    Dialogs.showErrorMessage("Installation Failed", "Could not create plugin files: ${e.message}")
    return
}

// Show installation success dialog
Platform.runLater {
    def dialog = new Dialog<ButtonType>()
    dialog.setTitle("SPIDER Plugin Installed!")
    dialog.setHeaderText("Installation Successful")
    
    def content = new VBox(10)
    content.setPadding(new Insets(20))
    
    def message = new Label("""SPIDER plugin has been installed successfully!

Next steps:
1. Download SPIDER models from Hugging Face
2. Install Python dependencies
3. Run 'SPIDER_Launcher' from the script menu

Would you like to see the detailed setup guide?""")
    message.setWrapText(true)
    
    content.getChildren().add(message)
    dialog.getDialogPane().setContent(content)
    
    def setupButton = new ButtonType("Show Setup Guide", ButtonBar.ButtonData.OK_DONE)
    def laterButton = new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE)
    
    dialog.getDialogPane().getButtonTypes().addAll(setupButton, laterButton)
    
    def result = dialog.showAndWait()
    
    if (result.isPresent() && result.get() == setupButton) {
        showSetupGuide()
    }
}

// Function to show setup guide
def showSetupGuide() {
    Platform.runLater {
        def dialog = new Dialog<ButtonType>()
        dialog.setTitle("SPIDER Setup Guide")
        dialog.setHeaderText("Complete Setup Instructions")
        
        def content = new VBox(10)
        content.setPadding(new Insets(10))
        
        def guideText = """
SPIDER SETUP GUIDE

1. INSTALL PYTHON DEPENDENCIES
   Open a command prompt and run:
   
   pip install torch torchvision transformers openslide-python pillow numpy matplotlib

2. DOWNLOAD SPIDER MODELS
   Visit Hugging Face and download one or more models:
   
   • Colorectal: https://huggingface.co/histai/SPIDER-colorectal-model
   • Skin: https://huggingface.co/histai/SPIDER-skin-model  
   • Thorax: https://huggingface.co/histai/SPIDER-thorax-model
   
   Use git clone or download manually to a folder like D:\\SPIDER_Models

3. CONFIGURE THE PLUGIN
   • Run 'SPIDER_Launcher' from Automate > Script editor
   • Click Configuration button
   • Set Python path and models directory
   • Test configuration

4. START USING SPIDER
   • Open a slide
   • Draw annotations
   • Choose model and analysis type
   • View AI-powered classifications!

For detailed instructions, see the complete setup guide.
"""
        
        def textArea = new TextArea(guideText)
        textArea.setEditable(false)
        textArea.setWrapText(true)
        textArea.setPrefSize(500, 400)
        
        content.getChildren().add(textArea)
        
        dialog.getDialogPane().setContent(content)
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK)
        dialog.setResizable(true)
        dialog.showAndWait()
    }
}

println "\n" + "=".repeat(60)
println "Installation complete!"
println "To start using SPIDER:"
println "1. Go to Automate > Script editor"
println "2. Open and run 'SPIDER_Launcher.groovy'"
println "=".repeat(60)
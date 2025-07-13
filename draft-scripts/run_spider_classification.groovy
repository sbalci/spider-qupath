// run_spider_classification.groovy
// Run SPIDER model and apply classifications to annotations

import qupath.lib.objects.classes.PathClass
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.io.GsonTools
import qupath.lib.objects.classes.PathClassFactory
import com.google.gson.JsonParser

// Define paths
def projectPath = buildFilePath(PROJECT_BASE_DIR)
def pythonPath = "D:\\DigitalPathologyDrafts\\.venv\\Scripts\\python.exe"  // Update this to your Python path
def outputPath = buildFilePath(projectPath, "spider_output")
def scriptPath = buildFilePath(projectPath, "spider_qupath_classifier.py")
def modelPath = "D:\\histai\\SPIDER-colorectal-model"  // Update this to your SPIDER model path
def tempAnnotationsPath = buildFilePath(outputPath, "annotations_to_predict.json")

// Create output directory
def outputDir = new File(outputPath)
if (!outputDir.exists())
    outputDir.mkdirs()

// Check if annotations file exists
def annotationsFile = new File(tempAnnotationsPath)
if (!annotationsFile.exists()) {
    Dialogs.showErrorMessage("Error", "No annotations file found. Please run the export_annotations script first.")
    return
}

// Run SPIDER classifier
def command = [pythonPath, scriptPath, tempAnnotationsPath, modelPath, outputPath]
println("Running command: " + command.join(" "))

def process = new ProcessBuilder(command)
    .redirectErrorStream(true)
    .start()

// Read and print the output
def reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
def line
while ((line = reader.readLine()) != null) {
    println(line)
}

def exitCode = process.waitFor()
println("Python process finished with exit code: " + exitCode)

if (exitCode == 0) {
    println("Classification completed successfully!")
    
    // Load predictions using Gson
    def predictionsPath = buildFilePath(outputPath, "predictions.json")
    def predictionsFile = new File(predictionsPath)
    
    if (!predictionsFile.exists()) {
        Dialogs.showErrorMessage("Error", "Predictions file not found at " + predictionsPath)
        return
    }
    
    def jsonText = predictionsFile.text
    def jsonParser = new JsonParser()
    def jsonArray = jsonParser.parse(jsonText).getAsJsonArray()
    
    // Convert to a list structure we can work with
    def predictions = []
    def gson = GsonTools.getInstance(true)
    for (int i = 0; i < jsonArray.size(); i++) {
        def predictionObj = jsonArray.get(i).getAsJsonObject()
        def prediction = [
            id: predictionObj.get("id").getAsString()
        ]
        
        if (!predictionObj.get("prediction").isJsonNull()) {
            prediction.prediction = predictionObj.get("prediction").getAsString()
            
            // Extract probabilities
            def probsObj = predictionObj.get("probabilities").getAsJsonObject()
            def probs = [:]
            probsObj.keySet().each { className ->
                probs[className] = probsObj.get(className).getAsDouble()
            }
            prediction.probabilities = probs
        } else {
            prediction.prediction = null
            prediction.probabilities = null
        }
        
        predictions.add(prediction)
    }
    
    // Get all annotations (not just unclassified ones)
    def allAnnotations = getAnnotationObjects()
    
    // Function to find or create a PathClass by name
    def findOrCreatePathClass = { String name ->
        def pathClass = PathClassFactory.getPathClass(name)
        if (pathClass == null) {
            // Create default colors based on class types
            def color = null
            if (name.toLowerCase().contains("adenocarcinoma")) {
                color = getColorRGB(220, 20, 60) // Crimson
            } else if (name.toLowerCase().contains("adenoma")) {
                color = getColorRGB(255, 140, 0) // Dark Orange
            } else if (name.toLowerCase().contains("stroma")) {
                color = getColorRGB(50, 205, 50) // Lime Green
            } else if (name.toLowerCase().contains("muscle")) {
                color = getColorRGB(139, 0, 139) // Dark Magenta
            } else if (name.toLowerCase().contains("vessel")) {
                color = getColorRGB(0, 0, 255)  // Blue
            } else {
                color = getColorRGB(128, 128, 128) // Gray default
            }
            pathClass = PathClassFactory.getPathClass(name, color)
        }
        return pathClass
    }
    
    // Map by ID for quick lookup
    def annotationMap = [:]
    allAnnotations.each { annotation ->
        annotationMap[annotation.getID().toString()] = annotation
    }
    
    // Apply classes based on predictions
    def applied = 0
    
    predictions.each { prediction ->
        def annotation = annotationMap[prediction.id]
        if (annotation != null && prediction.prediction != null) {
            // Get or create the class
            def pathClass = findOrCreatePathClass(prediction.prediction)
            
            // Apply class
            annotation.setPathClass(pathClass)
            
            // Add confidence information as a comment
            def confidence = String.format("%.1f", prediction.probabilities[prediction.prediction] * 100)
            def comment = "SPIDER: " + prediction.prediction + " (" + confidence + "%)"
            annotation.setName(comment)
            
            // Add measurements for probabilities
            prediction.probabilities.each { className, probability ->
                annotation.measurements.put("SPIDER: P(" + className + ")", probability)
            }
            
            applied++
            
            // Debug info
            println("Applied class " + prediction.prediction + " to annotation " + prediction.id)
        } else {
            if (annotation == null) {
                println("WARNING: Could not find annotation with ID " + prediction.id)
            }
        }
    }
    
    // Update display
    if (applied > 0) {
        fireHierarchyUpdate()
        println("Applied classifications to " + applied + " annotations")
        Dialogs.showInfoNotification("SPIDER", "Successfully classified " + applied + " annotations")
    } else {
        Dialogs.showErrorMessage("SPIDER", "Failed to apply classifications")
    }
} else {
    Dialogs.showErrorMessage("Error", "SPIDER classification failed. Check the log for details.")
}
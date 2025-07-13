// apply_spider_classifications.groovy
// Apply SPIDER classifications from predictions to unclassified annotations

import qupath.lib.objects.PathObject
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.io.GsonTools
import qupath.lib.objects.classes.PathClassFactory
import com.google.gson.JsonParser

// Get QuPath GUI instance
def qupath = QuPathGUI.getInstance()
if (qupath == null) {
    println("Error: Cannot get QuPath instance!")
    return
}

// Get the current viewer
def viewer = qupath.getViewer()
if (viewer == null) {
    Dialogs.showErrorMessage("SPIDER", "No viewer is available!")
    return
}

// Get current image data
def imageData = viewer.getImageData()
if (imageData == null) {
    Dialogs.showErrorMessage("SPIDER", "No image is open!")
    return
}

// Define paths
def outputPath = "D:/spider-qupath/spider_output" // IMPORTANT: Update to match your output path
def predictionsPath = new File(outputPath, "predictions.json").getPath()

// Check if predictions file exists
if (!new File(predictionsPath).exists()) {
    Dialogs.showErrorMessage("SPIDER", "No predictions found at: " + predictionsPath)
    return
}

// Get unclassified AND classified annotations (we'll match by ID)
def hierarchy = imageData.getHierarchy()
def allAnnotations = hierarchy.getAnnotationObjects()

if (allAnnotations.isEmpty()) {
    Dialogs.showErrorMessage("SPIDER", "No annotations found in the current image!")
    return
}

println("Found " + allAnnotations.size() + " total annotations")

// Load predictions
def jsonText = new File(predictionsPath).text
def jsonParser = new JsonParser()
def jsonArray = jsonParser.parse(jsonText).getAsJsonArray()

// Convert to a list structure we can work with
def predictions = []
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

println("Loaded " + predictions.size() + " predictions")

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

// Create a map of annotations by ID for quick lookup
def annotationMap = [:]
allAnnotations.each { annotation ->
    annotationMap[annotation.getID().toString()] = annotation
}
def appliedCount = 0

// Apply predictions to annotations
predictions.each { prediction ->
    def annotation = annotationMap[prediction.id]
    if (annotation != null && prediction.prediction != null) {
        // Set the classification
        def pathClass = findOrCreatePathClass(prediction.prediction)
        if (pathClass != null) {
            annotation.setPathClass(pathClass)
            appliedCount++
            
            // Add confidence information as a comment
            def confidence = String.format("%.1f", prediction.probabilities[prediction.prediction] * 100)
            def comment = "SPIDER: " + prediction.prediction + " (" + confidence + "%)"
            annotation.setName(comment)
            
            // Debug info
            println("Applied class " + prediction.prediction + " to annotation " + prediction.id)
        } else {
            println("WARNING: Could not create class " + prediction.prediction)
        }
    } else {
        if (annotation == null) {
            println("WARNING: Could not find annotation with ID " + prediction.id)
        }
    }
}

// Refresh the display
println("Applied classes to " + appliedCount + " annotations")
hierarchy.fireHierarchyChangedEvent(this)

// Show message with results
if (appliedCount > 0) {
    Dialogs.showInfoNotification("SPIDER", "Successfully classified " + appliedCount + " annotations")
} else {
    Dialogs.showErrorMessage("SPIDER", "Failed to apply classifications to annotations. Check the log for details.")
}
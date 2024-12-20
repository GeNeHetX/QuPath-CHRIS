// Hyper parameters
String classifier_name = "PANCK_240213" // Pixel classifier name
String label_name = "Tumor" // Annotation selection name
double minimum_object_size = 1.5 // Minimum size for created objects
double minimum_hole_size = 2 // Minimum whole size inside a detected object else it will be filled
double downsample = 8 // Define how much to downsample during export (may be required for large images)

// Use the pixel classifier to create annotation, (classifierName, minimumObjectSize, minimumHoleSize)
println("Creating annotations using the pixel classifier " + classifier_name)
createAnnotationsFromPixelClassifier(classifier_name, minimum_object_size, minimum_hole_size)
println("Finished creating annotations")

def imageData = getCurrentImageData()

// Define output path (relative to project)
def outputDir = buildFilePath(PROJECT_BASE_DIR, 'export')
mkdirs(outputDir) // Create the output directory
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName()) // Extract the name of the image
def path = buildFilePath(outputDir, name + "-all.png") // Create the output path



// Create an ImageServer where the pixels are derived from annotations
def labelServer = new LabeledImageServer.Builder(imageData)
  .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
  .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
  .addLabel(label_name, 255)      // Choose output labels (the order matters!)
  .multichannelOutput(false) // If true, each label refers to the channel of a multichannel binary image (required for multiclass probability)
  .build()

// Write the image
println("Writing the image mask")
writeImage(labelServer, path)
println("Done.") // Anounnce the termination of the script
println("Output path: " + path) // Print the output path
// Hyper parameters
String classifier_name = "CD8_full" // Pixel classifier name
double minimum_object_size = 1.5 // Minimum size for created objects
double minimum_hole_size = 2 // Minimum whole size inside a detected object else it will be filled
double downsample = 8 // Define how much to downsample during export (may be required for large images)
int tiling_factor = 4 // Size of image to size of the tile
String name = "test01" // Name of the mask

// Detect the current image data
def imageData = getCurrentImageData()

// Define output path (relative to project)
def outputDir = buildFilePath(PROJECT_BASE_DIR, 'export')

// Create the output directory
mkdirs(outputDir)

// Extract the name of the image
def img_name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())

// Get the current image server
def img = getCurrentServer()

// Get the image width and height in pixels
int img_width = img.getWidth()
int img_height = img.getHeight()

// Define the tile dimensions as image size divided by the tiling factor
int tile_width = img_width.intdiv(tiling_factor)
int tile_height = img_height

// Print the results
print "Image size: " + "(" + img_width + ", " + img_height + ")"  + " px"
print "Tile size: " + "(" + tile_width + ", " + tile_height + ")"  + " px"

// Get the image plane
def plane = ImagePlane.getDefaultPlane()

// Correct the maximum height and width to avoid missing the image boundaries
corrector = (tiling_factor - 1) / (tiling_factor)
maximum_height = img_height - corrector*tile_height
maximum_width = img_width - corrector*tile_width

for (int y = 0; y < maximum_height ; y += tile_height) {
    for (int x = 0; x < maximum_width; x += tile_width) {
        // print the image tile location
        print "tile" + "(" + x + ", " + y + ")"
        // Create a rectangle ROI
        def roi = ROIs.createRectangleROI(x, y, tile_width, tile_height, plane)
        // Create an annotation object from the ROI
        def annotation = PathObjects.createAnnotationObject(roi)
        // Set the ROI as the current selection
        setSelectedObject(annotation)
        // Add the annotation object to the annotations
        addObject(annotation)
        // Use the pixel classifier to create annotation, (classifierName, minimumObjectSize, minimumHoleSize)
        println("Creating annotations using the pixel classifier " + classifier_name)
        createAnnotationsFromPixelClassifier(classifier_name, minimum_object_size, minimum_hole_size)
        println("Finished creating annotations")
    }
}

// Create the output path
def path = buildFilePath(outputDir, img_name + "-" + name + ".png")

// Create an ImageServer where the pixels are derived from annotations
def labelServer = new LabeledImageServer.Builder(imageData)
  .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
  .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
  .addLabel('Immune cells', 255)      // Choose output labels (the order matters!)
  .multichannelOutput(false) // If true, each label refers to the channel of a multichannel binary image (required for multiclass probability)
  .build()

// Write the image
println("Writing the image mask")
writeImage(labelServer, path)

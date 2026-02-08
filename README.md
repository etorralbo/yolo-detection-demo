# YOLO Detection Demo

A standalone Android demo app showcasing real-time object detection using TensorFlow Lite YOLO11 with Google Play Asset Delivery.

## Features

- **Real-time Object Detection**: YOLO11 Nano model detects 80 COCO object classes
- **GPU Acceleration**: TensorFlow Lite GPU delegate with CPU fallback
- **Smart Object Selection**: Hysteresis-based tracking prevents flickering
- **Smooth UX**: 4-second display + 1-second cooldown for stable detection
- **Visual Feedback**: Bounding boxes and confidence labels
- **Google Play Asset Delivery**: On-demand model download (2.9 MB)

## Project Structure

```
yolo-detection-demo/
├── app/                           # Main application module
│   └── src/main/kotlin/com/yolo/demo/
│       ├── ui/                    # Compose UI screens
│       │   ├── AppScreen.kt       # Main app orchestration
│       │   ├── CameraPreviewScreen.kt  # Camera + detection display
│       │   ├── BoundingBoxOverlay.kt   # Bounding box drawing
│       │   └── DetectionLabel.kt       # Label chip
│       ├── camera/                # CameraX integration
│       │   ├── CameraManager.kt          # Camera lifecycle
│       │   ├── YoloCameraAnalyzer.kt     # Frame analysis
│       │   └── BoundingBoxTransformer.kt # Coordinate transformation
│       ├── yolo/                  # YOLO detection pipeline
│       │   ├── YoloObjectDetectionAnalyzer.kt  # Main pipeline
│       │   ├── inference/         # TensorFlow Lite inference
│       │   ├── preprocessing/     # Image preprocessing
│       │   ├── postprocessing/    # Non-Maximum Suppression
│       │   ├── selection/         # Smart object selection
│       │   ├── assetdelivery/     # Model download
│       │   └── utils/             # Image utilities
│       ├── viewmodel/             # State management
│       │   ├── MainViewModel.kt   # Model download orchestration
│       │   └── DetectionViewModel.kt  # Detection display timing
│       └── di/                    # Koin dependency injection
└── assets_delivery/               # Asset pack module
    └── assets/
        └── yolo11n_int8.tflite   # YOLO model (2.9 MB)
```

## Requirements

- **Android Studio**: Latest stable version
- **Min SDK**: 28 (Android 9)
- **Target SDK**: 36 (Android 16)
- **JDK**: 17+
- **Device**: Physical device recommended for camera testing

## Building & Running

### Option 1: Install via bundletool (Recommended for Asset Delivery Testing)

**Important**: APK installs (Android Studio Run, `./gradlew installDebug`) do NOT support Play Asset Delivery. Use bundletool for local testing.

#### Prerequisites
Install bundletool via Homebrew:
```bash
brew install bundletool
```

Verify installation:
```bash
bundletool version
```

#### Installation Steps

1. **Generate the AAB**:
   ```bash
   ./gradlew bundleDebug
   ```

2. **Set paths**:
   ```bash
   AAB_DIR="$PWD/app/build/outputs/bundle/debug"
   AAB="$(ls "$AAB_DIR"/*.aab | head -n 1)"
   APKS="$AAB_DIR/app-local-testing.apks"
   ```

3. **Uninstall previous version** (if exists):
   ```bash
   adb uninstall com.yolo.demo
   ```

4. **Generate APKS with local testing enabled**:
   ```bash
   bundletool build-apks \
     --bundle="$AAB" \
     --output="$APKS" \
     --local-testing
   ```

5. **Install on device**:
   ```bash
   bundletool install-apks --apks="$APKS"
   ```

6. **Verify installation** (should show multiple split APKs):
   ```bash
   adb shell pm path com.yolo.demo
   ```
   ✅ Expected: Multiple `package:` entries (base.apk + splits)
   ❌ If only one entry: Asset Delivery won't work

### Option 2: Google Play Console (Production Testing)

1. Generate signed release AAB:
   ```bash
   ./gradlew bundleRelease
   ```

2. Upload to Google Play Console → Internal Testing

3. Install via Play Store on test device

## Architecture

### Detection Pipeline

```
CameraX Frame (ImageProxy, YUV420)
    ↓
Convert to Bitmap (RGB_565)
    ↓
Rotate if needed (match device orientation)
    ↓
Preprocess (640x640, normalize [0,1])
    ↓
YOLO Inference (TensorFlow Lite + GPU)
    ↓
Parse Output (8400 detections → bounding boxes)
    ↓
Non-Maximum Suppression (IoU threshold 0.45)
    ↓
Object Selection Strategy (hysteresis + tracking)
    ↓
Display for 4 seconds + 1 second cooldown
    ↓
UI Update (bounding box + label)
```

### Key Components

**YOLO Model**: YOLO11 Nano INT8 quantized
- Input: 640x640 RGB normalized to [0,1]
- Output: 8400 anchor boxes with 84 features each
- Classes: 80 COCO objects (person, car, phone, etc.)

**GPU Acceleration**:
- Uses TensorFlow Lite GPU delegate when available
- Graceful fallback to CPU with proper error handling
- Proguard rules prevent class stripping

**Smart Object Selection**:
- 70% center proximity + 30% size scoring
- 1.3x hysteresis threshold prevents rapid switching
- 15-frame cooldown between reconsiderations
- Tracks objects by ID for stable selection

**Display Timing**:
- **4-second display**: Shows detected object with bounding box
- **1-second cooldown**: Brief pause before next detection
- Prevents UI flickering and provides smooth UX

## Dependencies

### Core
- **Kotlin**: 2.1.0
- **Jetpack Compose**: Material3, BOM 2024.12.01
- **CameraX**: 1.5.2 (camera-compose for native integration)
- **Koin**: 4.0.1 (dependency injection)
- **Coroutines**: 1.9.0

### ML & Asset Delivery
- **TensorFlow Lite**: 2.17.0 (with GPU support)
- **Play Asset Delivery**: 2.2.2

### Testing
- **JUnit**: 4.13.2
- **Mockk**: 1.13.13
- **Turbine**: 1.2.0 (Flow testing)
- **Robolectric**: 4.14.1 (Android framework testing)

## Configuration

### YOLO Settings
```kotlin
// Inference
INPUT_SIZE = 640
CONFIDENCE_THRESHOLD = 0.75f

// Non-Maximum Suppression
IOU_THRESHOLD = 0.45f

// Object Selection
HYSTERESIS_MULTIPLIER = 1.3f
COOLDOWN_FRAMES = 15
CENTER_WEIGHT = 0.7f
AREA_WEIGHT = 0.3f

// Display Timing
DISPLAY_DURATION = 4000ms
COOLDOWN_DURATION = 1000ms
```

### GPU Delegate
GPU acceleration is attempted first, with automatic CPU fallback:
- **GPU mode**: 2 threads (GPU handles most work)
- **CPU mode**: 4 threads (more parallelism needed)

## Testing

Run all unit tests:
```bash
./gradlew test
```

**Test Coverage**:
- ✅ MainViewModel: Model download flow
- ✅ YoloOutputParser: Coordinate conversion, class selection
- ✅ NonMaximumSuppression: Overlap filtering
- ✅ ObjectSelectionStrategy: Hysteresis, tracking

**Total**: 24 tests, all passing

## Implementation History

This project was built in 6 progressive steps:

1. **Step 1**: Hello World Android app
2. **Step 2**: Camera preview with CameraXViewfinder
3. **Step 3**: Google Play Asset Delivery with error handling
4. **Step 4**: TensorFlow Lite inference engine
5. **Step 5**: Complete YOLO pipeline with GPU delegate fix
6. **Step 6**: UI with bounding boxes and timing logic

Each step is on a separate git branch with working functionality.

## Troubleshooting

### Asset Delivery Issues
- **Model not downloading**: Ensure app is installed via Play Store or bundletool
- **Download fails**: Check network connection and Play Services
- **Model not found**: Check logs for asset pack status

### Performance Issues
- **Slow inference**: Check if GPU delegate initialized (look for logs)
- **High battery drain**: Normal for real-time ML inference
- **Frame drops**: Reduce camera resolution or inference frequency

### Detection Quality
- **No detections**: Adjust `CONFIDENCE_THRESHOLD` (currently 0.75)
- **Too many boxes**: Reduce `IOU_THRESHOLD` for stricter NMS
- **Unstable selection**: Increase `HYSTERESIS_MULTIPLIER` or `COOLDOWN_FRAMES`

## License

This is a demonstration project extracted from Back Market's Android app for educational purposes.

## References

- **YOLO11**: [Ultralytics YOLO11 Documentation](https://docs.ultralytics.com/models/yolo11/)
- **TensorFlow Lite**: [Android ML Kit](https://www.tensorflow.org/lite/android)
- **CameraX**: [Android CameraX Guide](https://developer.android.com/training/camerax)
- **Play Asset Delivery**: [Google Play Asset Delivery](https://developer.android.com/guide/playcore/asset-delivery)

package ar.core.ARVIAL.geospatial

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


class GeospatialViewModel: ViewModel() {

    // TODO - aniadir hora a cada ubicación?º
    private val locationHistory = mutableListOf<Pair<LocalDateTime, Coords>>(Pair(LocalDateTime.now(), Coords(0.0, 0.0)), Pair(LocalDateTime.now(), Coords(1.0, 1.0)))
    private val lockLocation = Any()

    private val formatterES = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale("es", "ES")) // Or Locale("es", "ES") for Spain Spanish

    fun addLocationToHistory(location: Coords) {
        val entry = Pair(LocalDateTime.now(), location)
        synchronized(lockLocation) {
            locationHistory.add(entry)
        }
    }

    // TODO
    fun saveRoute(context: Context) {

        if (locationHistory.isEmpty())
            Toast.makeText(context, "Location History empty", Toast.LENGTH_SHORT).show()

//        else {
//            Toast.makeText(context, "Trying to save Location History", Toast.LENGTH_SHORT).show()
//        }

        var csvText = ""
        viewModelScope.launch(Dispatchers.IO) {
            synchronized(lockLocation) {
                for (historyEntry in locationHistory) {
                    val time = historyEntry.first
                    val location = historyEntry.second
                    csvText += "${location.lat}, ${location.lon}, ${location.alt}\n"
                }
    //            shareCsvFile(context, csvText, fileName = LocalDateTime.now().format(formatterES))
    //            TODO - Uncomment when done testing
    //            locationHistory.clear()
            }

            saveCsvToInternalStorage(
                context,
                csvText,
                fileName = LocalDateTime.now().format(formatterES)
            )
        }
    }

    private suspend fun saveCsvToInternalStorage(context: Context, csvString: String, fileName: String) {
        val file = File(
            context.filesDir,
            fileName
        ) // getFilesDir() points to your app's internal storage directory
        try {
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { fos ->
                    fos.write(csvString.toByteArray(StandardCharsets.UTF_8))
                    Log.d("FileSaver", "CSV file saved to internal storage: " + file.absolutePath)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "CSV $fileName saved", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        } catch (e: IOException) {
            Log.e("FileSaver", "Error saving CSV to internal storage: " + e.message)
//            return false
        }
    }

    fun saveCsvToFile(context: Context, csvContent: String, fileName: String): Boolean {
        // Get the directory for your app's private files on the internal storage.
        // This directory is specific to your app and is automatically created.
        // Files saved here are not accessible by other apps.
        val filesDir: File = context.filesDir

        // Create a File object for the specified file name within the app's internal storage.
        val file = File(filesDir, fileName)

        try {
            // Write the CSV content to the file.
            // writeText handles opening, writing, and closing the file automatically.
            file.writeText(csvContent)
            // Log a success message. In a real Android app, you might use Log.d()
            // For simplicity, using println here.
            println("CSV content successfully saved to: ${file.absolutePath}")
            return true
        } catch (e: IOException) {
            // Catch any IO exceptions that might occur during file writing (e.g., disk full).
            System.err.println("Error saving CSV to file: ${e.message}")
            e.printStackTrace()
            return false
        } catch (e: SecurityException) {
            // This catch block is less likely for internal storage but good for robustness.
            System.err.println("Security error saving CSV to file: ${e.message}. Check app permissions if writing to external storage.")
            e.printStackTrace()
            return false
        }
    }

    private fun shareCsvFile(context: Context, csvContent: String, fileName: String = "my_route.csv") {
        // 1. Create the CSV file
        val file = File(context.cacheDir, fileName) // Use cacheDir for temporary files
        try {
            FileOutputStream(file).use { outputStream ->
                outputStream.write(csvContent.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error (e.g., show a Toast)
            return
        }

        // 2. Get a content URI from the FileProvider
        val fileUri: Uri? = try {
            // IMPORTANT: The authority MUST match what you defined in AndroidManifest.xml
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: IllegalArgumentException) {
//            e.printStackTrace()
            // Handle error (e.g., show a Toast)
//            Toast.makeText(context, "ERROR ${context.packageName}.provider", Toast.LENGTH_SHORT).show()
            Log.e("GeospatialViewModel", e.message!!)
            return
        }

        // Ensure the URI is not null
        if (fileUri != null) {
            // 3. Create the share Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv" // Set the MIME type for CSV files
                putExtra(Intent.EXTRA_STREAM, fileUri) // Attach the file URI
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant read access to the receiving app
                // Optional: Add a subject
                putExtra(Intent.EXTRA_SUBJECT, "Share CSV Data from My App")
                // Optional: Add some explanatory text
                putExtra(Intent.EXTRA_TEXT, "Here's the CSV data from my application.")
            }

            // 4. Start the chooser
            // Always use createChooser to ensure the user gets a choice of apps
            val chooserIntent = Intent.createChooser(shareIntent, "Share CSV using...")
            context.startActivity(chooserIntent)
        }
        else {
            Toast.makeText(context, "ERROR fileUri is null", Toast.LENGTH_SHORT).show()
        }
    }

// How to call this function (e.g., from an Activity or a Composable):
// val myCsvData = "Header1,Header2\nValue1,Value2\nValue3,Value4"
// shareCsvFile(this, myCsvData, "report.csv") // 'this' if inside an Activity

}
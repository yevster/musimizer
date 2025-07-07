# Musimizer

Musimizer selects random albums from your music collection. It only works if the collection is organized into an "Artist/Album" folder structure (the way ITunes does it).

When you first run it, it will ask you for the location of your music collection. This should be the folder that has artist folders inside it.

After that, it will show you a random list of albums. If you don't want to listen to a particular album or have already listened to it, click the Exclude button to the right of the album, and Musimizer will never show it to you again. You can also bookmark albums for future listening and click the "Play" button to launch them in your default audio player.

---

## Notes
- For issues or suggestions, please open an issue on the [GitHub Issues page](https://github.com/yevster/musimizer/issues).
- Most of this code (and all of the bugs) is AI generated. Read and use it at your peril.

---

## How to Run Musimizer

This is a pet project, so I haven't really put in the effort to generate easy installers aside from trying to get AI to do it and having it fail repeatedy. So instead you get to do this:

### Download the Application
- Go to the [Releases page](https://github.com/yevster/musimizer/releases) on GitHub.
- Download the latest zip file for your platform (e.g., `musimizer-1.0.5.zip`).

### Extract the Zip File
- Unzip the downloaded archive to a folder of your choice.

### Run on Windows
1. Open the extracted folder.
2. Navigate to the `bin` directory.
3. Double-click on `musimizer.bat` to launch the app. (Note: Windows may warn you that it's kind of shady to run random apps from strangers. It's not entirely wrong. 😉)

Or...

1. Create a shortcut on the desktop to "javaw.exe" inside the `bin` directory (wherever you extracted the zip file contents).
1. Edit the shortcut and add the following to the end of the `Target` field: `-m com.musimizer/com.musimizer.MainApp`.

I prefer the second approach.

### Run on macOS
1. Open a Terminal window.
2. Navigate to the extracted folder and then into the `bin` directory:
   ```sh
   cd /path/to/musimizer-jlink/bin
   ```
3. Run the launcher script:
   ```sh
   ./musimizer
   ```
   - If you get a permission error, you may need to make the file executable:
     ```sh
     chmod +x ./musimizer
     ./musimizer
     ```

I don't have a mac to test this on, so let me know if it doesn't work.
---

## Building from Source
If you want to build Musimizer yourself, you need [Maven](https://maven.apache.org/) and JDK 21+ installed. Then run:
```sh
mvn clean javafx:jlink
```
The output will be in `target/musimizer-jlink`.

## License
Musimizer is open source and distributed under the Apache-2.0 License.

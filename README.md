# IA-epfl
Repository for the programming exercises of the Intelligent Agents course (CS-430) at EPFL.

Each folder corresponds to one exercise and inside contains the source code but also the report, especifications and the needed libraries in the folder `lib/`.

## Configuring Eclipse

Open an existing project with the folder corresponding to each exercise as root of the project .

### Add external JARs

Right click in the Package Explorer > Build Path > Configure Build Path...

Move to the Libraries tab and click Add external JARs. Then go to the folder of the exercise and import the JARs in the `lib/` folder.

### Adding Javadoc to external JARs

It's useful to at least add the Javadoc for Repast, which is a zip file located in the `lib/` folder. Open the Referenced Libraries in the Package Explorer, Right click Repast > Properties > Javadoc Location.

Select Javadoc in archive and browse to find the zip folder with the documentation in the Archive Path. Then, browse in Path within archive and select the sub-folder `api`. Finally validate and if it's correctly imported hit Apply and close.

### Creating runnable JAR
To create a new runnable JAR file in the workbench:

1. From the menu bar's File menu, select Export.
2. Expand the Java node and select Runnable JAR file. Click Next.
3. In the Launch configuration, select a 'Java Application' launch configuration to use to create a runnable JAR.
4. In the Export destination field, either type or click Browse to select a location for the JAR file.
5. Select an appropriate library handling strategy. In case of doubt, Package required libraries into generated JAR.

You may have troubles with the Launch configuration. If the error`could not find main method from given launch configuration` is prompted, click Run configurations (that will show up when clicking the slider at the right of the right button) and then delete all the current Java Applications. Once you run your code, with the appropriate class with a main class, the correct configuration will appear. You can now generate the JAR file as explained above.

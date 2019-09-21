# IA-epfl
Repository for the programming exercises of the Intelligent Agents course (CS-430) at EPFL.

* `doc/` Contains the specifications of each exercise
* The other folders correspond to the code for each of these exercises

## Configuring Eclipse

Open an existing project with barbany-bolon-in as root.

### Add external JARs

Right click in the Package Explorer > Build Path > Configure Build Path...

Move to the Libraries tab and click Add external JARs. Then go to the folder of the exercise and import the JARs in the `lib/` folder.

### Adding Javadoc to external JARs

It's useful to at least add the Javadoc for Repast, which is a zip file located in the `lib/` folder. Open the Referenced Libraries in the Package Explorer, Right click Repast > Properties > Javadoc Location.

Select Javadoc in archive and browse to find the zip folder with the documentation in the Archive Path. Then, browse in Path within archive and select the sub-folder `api`. Finally validate and if it's correctly imported hit Apply and close.


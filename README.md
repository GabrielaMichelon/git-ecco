# Mining features life cycle

## First steps

* Clone this repository: `git clone https://github.com/GabrielaMichelon/git-ecco.git`
* Checkout to branch mining: `git checkout mining`
* Clone the git reposiritory of the target sytems in a folder on your computer
 - For example: LibSSH git repository: https://gitlab.com/libssh/libssh-mirror.git

## Requires

* JDK 8
* [Gradle](http://gradle.org/ "Gradle") &#8805; 4.4 as build system

You can add jdk and gradle as environmental variables to make easier the script execution in the command line

## Execute project with IDE

* IntelliJ supports Gradle out of the box. Just import the project as a Gradle project
* translation* module contains a test ``MiningMetricsTest.java``, which you can execute for mining the feature life cycle of all commits of a system or of a specific release.

## Execut from the command line

* Open the directory of the build file in `module translation` [build file](https://github.com/GabrielaMichelon/git-ecco/blob/mining/translation/build.gradle)
* Parameters
  - **First paramenter**: the folder containing the Git repository of the system you want to mine the features life cycle
  - **Second paramenter**: the folder where you want to store the resulted data from mining features life cycle
  - **Third parameter**: **'0'** for analyzing all releases from the first one, or  **'1'** for analyzing all releases from a specific one
  - **Fourth parameter** (`mandatory` if the third paramenter is **'1'**, `optional` if the third paramenter is **'0'**): **'\<release_name>'**
  
* Type the following command
 - gradle run -Pmyargs='\<system Git folder>','\<folder to store results>','0'
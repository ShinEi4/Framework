@echo off
setlocal EnableDelayedExpansion

rem Définir le chemin d'accès au répertoire des sources et au répertoire de destination des fichiers compilés
set "sourceDirectory=src"
set "destinationDirectory=bin"

rem Chemin vers le répertoire contenant les bibliothèques nécessaires
set "libDirectory=lib"  

rem Initialiser la liste des fichiers Java à compiler
set "javaFiles="

rem Récupérer la liste de tous les fichiers Java dans les sous-dossiers de %sourceDirectory%
for /r "%sourceDirectory%" %%G in (*.java) do (
    rem Extraire la structure des packages à partir du chemin complet du fichier source
    set "javaFile=%%~fG"
    set "packagePath=!javaFile:%sourceDirectory%=!"
    set "packagePath=!packagePath:~0,-\%%~nG%%~xG!"

    rem Créer les répertoires de sortie si nécessaire
    if not exist "%destinationDirectory%!packagePath!" (
        mkdir "%destinationDirectory%!packagePath!" >nul
    )
    
    rem Ajouter le fichier Java à la liste des fichiers à compiler
    set "javaFiles=!javaFiles! "%%G""
)

rem Construire le chemin de classe pour toutes les bibliothèques dans le dossier "lib"
set "classpath="
for %%I in ("%libDirectory%\*.jar") do (
    set "classpath=!classpath!;"%%I""
)

rem Compiler tous les fichiers Java en une seule commande avec les bibliothèques nécessaires
javac -cp "%classpath%" -d "%destinationDirectory%" !javaFiles!


pause

rem Création du fichier JAR
echo Création du fichier JAR...
jar cvf app.jar -C "%destinationDirectory%" .

echo Fichier JAR créé avec succès : app.jar

rem Création du répertoire WEB-INF/lib s'il n'existe pas
if not exist "E:\S4\Web Dynamique\ProjetSrpint\Test\WEB-INF\lib" (
    mkdir "E:\S4\Web Dynamique\ProjetSrpint\Test\lib" >nul
    echo Répertoire WEB-INF/lib créé avec succès.
)

rem Copier le fichier JAR vers le répertoire WEB-INF/lib
echo Copie du fichier JAR vers le répertoire WEB-INF/lib...
copy /Y "app.jar" "E:\S4\Web Dynamique\ProjetSrpint\Test\lib\"

echo Fichier JAR copié avec succès dans le répertoire WEB-INF/lib.
echo true

pause

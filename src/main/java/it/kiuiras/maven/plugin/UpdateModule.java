/**
 * Created on 04 September, 2019.
 */
package it.kiuiras.maven.plugin;

import it.kiuiras.maven.plugin.util.PomUtil;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.*;

/**
 * Updates the given module, all the submodules and all relative dependencies transitively.
 *
 * @author Andrea Grossi
 */
@Mojo(name = "update-module", requiresDirectInvocation = true, aggregator = true)
public class UpdateModule extends AbstractMojo {

    public static final String SNAPSHOT = "-SNAPSHOT";
    public static final String POM_XML = "pom.xml";

    private Map<String, File> allModulesPom;

    /**
     * The version type in the XX.YY.ZZ versioning system.
     * Where XX is MAJOR, YY is MINOR and ZZ is PATCH.
     */
    enum VersionType {
        MAJOR, MINOR, PATCH;
    }

    /**
     * The version type to increment. (Default is PATCH)
     */
    @Parameter(property = "versionType", defaultValue = "PATCH", required = true)
    private VersionType versionType;

    /**
     * Property to set to update parent of the updated modules.
     */
    @Parameter(property = "updateParent", defaultValue = "true")
    private boolean updateParent;

    /**
     * The Maven Project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Executes the Update Module goal.
     * <p>
     * This is the entry point of the plugin.
     *
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        try {
            File projectPom = new File(project.getBasedir(), POM_XML);
            getLog().info("The POM of project is: " + projectPom);
            getAllModules();
            LinkedHashMap<String, File> projectsToUpdate = new LinkedHashMap<>();
            projectsToUpdate.put(PomUtil.getModuleGA(projectPom), projectPom);
            ArrayList<File> pomList = new ArrayList<>(projectsToUpdate.values());
            for (int i = 0; i < pomList.size(); i++) {
                File pomToUpdate = pomList.get(i);
                updateModuleVersion(pomToUpdate).forEach((dmGa, dmPom) -> projectsToUpdate.putIfAbsent(dmGa, dmPom));
                String parentGA = PomUtil.getParentGA(pomToUpdate);
                if (updateParent && parentGA != null) {
                    projectsToUpdate.putIfAbsent(parentGA, allModulesPom.get(parentGA));
                }
                pomList = new ArrayList<>(projectsToUpdate.values());
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve all modules in this Maven project.
     */
    private void getAllModules() {
        File rootPom = getRootProjectPom();
        getLog().info("The POM of root is: " + rootPom);
        allModulesPom = getAllModules(rootPom);
    }

    /**
     * Gets the {@link Map} with the GA and the POM {@link File} of the submodules of the module defined by the given POM.
     *
     * @param pomToUpdate the pom of the parent module
     * @return the {@link Map} with the GA and the POM {@link File} of the submodules
     */
    private Map<String, File> getSubmodules(File pomToUpdate) {
        Map<String, File> submodules = new HashMap<>();
        try {
            allModulesPom.forEach((mGa, mPom) -> {
                try {
                    if (PomUtil.isSubmodule(pomToUpdate, mPom)) {
                        submodules.putIfAbsent(mGa, mPom);
                    }
                } catch (Exception e) {
                    getLog().error(e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
        }
        return submodules;
    }

    /**
     * Gets a map of POM of all submodules given the root POM.
     *
     * @param rootPom the root POM
     * @return the {@link Map} with module name and relative POM {@link File}
     */
    private Map<String, File> getAllModules(File rootPom) {
        Map<String, File> allModulesPom = new HashMap<>();
        try {
            String moduleGA = PomUtil.getModuleGA(rootPom);
            getLog().debug("Add module ".concat(moduleGA));
            allModulesPom.put(moduleGA, rootPom);
            List<File> files = Arrays.asList(Objects.requireNonNull(rootPom.getParentFile().listFiles()));
            files.forEach(f -> {
                if (f.isDirectory()) {
                    List<File> dirFiles = Arrays.asList(Objects.requireNonNull(f.listFiles()));
                    dirFiles.forEach(df -> {
                        if (df.getName().equalsIgnoreCase(POM_XML)) {
                            allModulesPom.putAll(getAllModules(df));
                        }
                    });
                }
            });
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
        }
        return allModulesPom;
    }

    /**
     * Gets the POM of the root.
     *
     * @return the {@link File} POM of the root
     */
    private File getRootProjectPom() {
        MavenProject rootProject = project;
        while (rootProject.hasParent()) {
            rootProject = rootProject.getParent();
        }
        return new File(rootProject.getBasedir(), POM_XML);
    }

    /**
     * Updates the version of the module given its POM {@link File} (if a version is specified) and return a {@link Map} of all dependants modules.
     * <p>
     * This modifies the version of the module in the given POM, in the parent section of the submodules POM and in the dependencies section of dependant modules POM.
     *
     * @param pom the POM to update
     * @return the {@link Map} with dependent modules.
     */
    private Map<String, File> updateModuleVersion(File pom) {
        Map<String, File> depModules = new HashMap<>();
        try {
            String currentVersion = PomUtil.getModuleVersion(pom);
            if (currentVersion != null && !currentVersion.isEmpty()) {
                String nextVersion = getNextVersion(currentVersion);
                getLog().info("Update project ".concat(PomUtil.getModuleGA(pom)).concat(" from version ")
                        .concat(currentVersion).concat(" to version ").concat(nextVersion));
                PomUtil.setModuleVersion(pom, nextVersion);
                String ga = PomUtil.getModuleGA(pom);
                allModulesPom.forEach((modGA, modPom) -> {
                    try {
                        if (PomUtil.isDependent(pom, modPom)) {
                            PomUtil.setDependencyVersion(modPom, ga, nextVersion);
                            getLog().info("Update ".concat(PomUtil.getModuleGA(modPom).concat(" dependency from ".concat(PomUtil.getModuleGA(pom)).concat(" to version ").concat(nextVersion))));
                            depModules.putIfAbsent(modGA, modPom);
                        }
                        if (PomUtil.isSubmodule(pom, modPom)) {
                            PomUtil.setParentVersion(modPom, nextVersion);
                            getLog().info("Update ".concat(PomUtil.getModuleGA(modPom).concat(" parent to version ").concat(nextVersion)));
                        }
                    } catch (Exception e) {
                        getLog().error(e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            getLog().error(e);
        }
        return depModules;
    }

    /**
     * Gets the next version to set accordingly to the given version and to the specified version type.
     *
     * @param version the current version
     * @return the next version to set
     */
    private String getNextVersion(String version) {
        boolean isSnapshot = version.endsWith(SNAPSHOT);
        if (isSnapshot) {
            version = version.substring(0, version.indexOf(SNAPSHOT));
        }
        String[] versions = version.split("\\.");
        int versionToSet = Integer.parseInt(versions[versionType.ordinal()]) + 1;
        versions[versionType.ordinal()] = Integer.toString(versionToSet);
        String nextVersion = StringUtils.join(versions, ".");
        if (isSnapshot) {
            nextVersion = nextVersion.concat(SNAPSHOT);
        }
        return nextVersion;
    }
}
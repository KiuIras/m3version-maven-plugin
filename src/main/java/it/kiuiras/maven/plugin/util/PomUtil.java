/**
 * Created on 06 September, 2019.
 */
package it.kiuiras.maven.plugin.util;

import org.joox.Match;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.joox.JOOX.$;

/**
 * {@link PomUtil} provides static utility methods to get information from a POM file and to modify it.
 *
 * @author Andrea Grossi
 */
public class PomUtil {

    /**
     * Gets the Group and Artifact of the module defined by the given POM {@link File}.
     *
     * @param pom the POM {@link File}
     * @throws IOException
     * @throws SAXException
     */
    public static String getModuleGA(File pom) throws IOException, SAXException {
        return getModuleGroupId(pom).concat(".").concat(getModuleArtifactId(pom));
    }

    /**
     * Gets the group id of the module defined by the given POM {@link File}.
     *
     * @param pom the POM {@link File}
     * @return the group id
     * @throws SAXException
     * @throws IOException
     */
    private static String getModuleGroupId(File pom) throws SAXException, IOException {
        Document cfg = $(pom).document();
        String groupId = $(cfg).child("groupId").content();
        if (groupId == null) {
            groupId = $(cfg).child("parent").child("groupId").content();
        }
        return groupId;
    }

    /**
     * Gets the artifact id of the module defined by the given POM {@link File}.
     *
     * @param pom the POM {@link File}
     * @return the artifact id
     * @throws SAXException
     * @throws IOException
     */
    private static String getModuleArtifactId(File pom) throws SAXException, IOException {
        Document cfg = $(pom).document();
        String artifactId = $(cfg).child("artifactId").content();
        return artifactId;
    }

    /**
     * Gets the version of the module defined by the given POM {@link File}
     *
     * @param pom the POM {@link File}
     * @return the version
     * @throws IOException
     * @throws SAXException
     */
    public static String getModuleVersion(File pom) throws IOException, SAXException {
        Document cfg = $(pom).document();
        String version = $(cfg).child("version").content();
        if (version == null) {
            version = $(cfg).child("parent").child("version").content();
        }
        return version;
    }

    /**
     * Sets the given version in the given POM {@link File}.
     *
     * @param pom     the POM file to change
     * @param version the version to set
     * @throws TransformerException
     * @throws IOException
     * @throws SAXException
     */
    public static void setModuleVersion(File pom, String version) throws TransformerException, IOException, SAXException {
        Document cfg = $(pom).document();
        if ($(cfg).child("version").isEmpty()) {
            $(cfg).child("artifactId").after("<version></version>");
        }
        $(cfg).child("version").content(version);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(cfg);
        StreamResult streamResult = new StreamResult(pom);
        transformer.transform(domSource, streamResult);
    }

    /**
     * Return true if a given module is a submodule of other given module.
     *
     * @param parentPom the candidate parent module POM {@link File}
     * @param pom       the candidate submodule POM {@link File}
     * @return true if is a submodule, false otherwise
     * @throws IOException
     * @throws SAXException
     */
    public static boolean isSubmodule(File parentPom, File pom) throws IOException, SAXException {
        boolean result = false;
        Document cfg = $(pom).document();
        if ($(cfg).child("parent").isNotEmpty()) {
            String pGroupId = getModuleGroupId(parentPom);
            String pArtifactId = getModuleArtifactId(parentPom);
            String subParentGroupId = $(cfg).child("parent").child("groupId").content();
            String subParentArtifactId = $(cfg).child("parent").child("artifactId").content();
            result = subParentGroupId.equals(pGroupId) && subParentArtifactId.equals(pArtifactId);
        }
        return result;
    }

    /**
     * Return true if a given module is a dependency of other given module.
     *
     * @param dependencyPom the candidate depencency module POM {@link File}
     * @param dependentPom  the candidate dependent module POM {@link File}
     * @return true if is a submodule, false otherwise
     * @throws IOException
     * @throws SAXException
     */
    public static boolean isDependent(File dependencyPom, File dependentPom) throws IOException, SAXException {
        AtomicBoolean result = new AtomicBoolean(false);
        Document cfg = $(dependentPom).document();
        String dependencyGroupId = getModuleGroupId(dependencyPom);
        String dependencyArtifactId = getModuleArtifactId(dependencyPom);
        Match dependencies = $(cfg).children("dependencies");
        // TODO This must be done with XML
        dependencies.contents().forEach(d -> {
            if (d.contains(dependencyArtifactId) && d.contains(dependencyGroupId)) {
                result.set(true);
            }
        });
        return result.get();
    }
}

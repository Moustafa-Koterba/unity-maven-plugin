package ca.mestevens.unity;

import ca.mestevens.unity.utils.DependencyGatherer;
import ca.mestevens.unity.utils.ProcessRunner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Goal which generates your framework dependencies in the target directory.
 *
 * @goal unity-library-dependencies
 * 
 * @phase initialize
 */
public class FrameworkDependenciesMojo extends AbstractMojo {

	/**
	 * @parameter property="project"
	 * @readonly
	 * @required
	 */
	public MavenProject project;

	/**
	 * The project's remote repositories to use for the resolution of project
	 * dependencies.
	 * 
	 * @parameter default-value="${project.remoteProjectRepositories}"
	 * @readonly
	 */
	protected List<RemoteRepository> projectRepos;

	/**
	 * The entry point to Aether, i.e. the component doing all the work.
	 * 
	 * @component
	 */
	protected RepositorySystem repoSystem;

	/**
	 * The current repository/network configuration of Maven.
	 * 
	 * @parameter default-value="${repositorySystemSession}"
	 * @readonly
	 */
	protected RepositorySystemSession repoSession;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Starting execution");
		
		DependencyGatherer dependencyGatherer = new DependencyGatherer(getLog(), project, projectRepos, repoSystem, repoSession);
		List<ArtifactResult> resolvedArtifacts = dependencyGatherer.resolveArtifacts();;
		
		File resultFile = new File(project.getBasedir() + "/Assets/Runtime/Plugins");
		try {
			if (resultFile.exists()) {
				FileUtils.deleteDirectory(resultFile);
			}
			FileUtils.mkdir(resultFile.getAbsolutePath());
		} catch (IOException e) {
			getLog().error("Problem deleting or creating plugin folder at: " + resultFile.getAbsolutePath());
			getLog().error(e.getMessage());
			throw new MojoFailureException("Problem deleting or creating plugin folder at: " + resultFile.getAbsolutePath());
		}
		
		for (ArtifactResult resolvedArtifact : resolvedArtifacts) {
			Artifact artifact = resolvedArtifact.getArtifact();
			final String typePropertyValue = artifact.getProperty("type", "");

			if (typePropertyValue.equals("unity-library") || typePropertyValue.equals("dll")) {
				this.copyArtifact(artifact, resultFile);
			}

			if (typePropertyValue.equals("unity-library")) {
				try {
					Artifact ab = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "ios-plugin",
							"ios-plugin", artifact.getVersion());
					ArtifactRequest request = new ArtifactRequest(ab, projectRepos, null);
					ArtifactResult artifactResult = repoSystem.resolveArtifact(repoSession, request);
					Artifact a = artifactResult.getArtifact();
					File zippedFile = a.getFile();
					File iOSPluginsFolder = new File(project.getBasedir() + "/Assets/Plugins/iOS");
					ProcessRunner processRunner = new ProcessRunner(getLog());
					processRunner.runProcess(null, "unzip", "-uo", zippedFile.getAbsolutePath(), "-d", iOSPluginsFolder.getAbsolutePath());
				} catch (ArtifactResolutionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					Artifact ab = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "android-plugin",
							"android-plugin", artifact.getVersion());
					ArtifactRequest request = new ArtifactRequest(ab, projectRepos, null);
					ArtifactResult artifactResult = repoSystem.resolveArtifact(repoSession, request);
					Artifact a = artifactResult.getArtifact();
					File zippedFile = a.getFile();
					File AndroidPluginsFolder = new File(project.getBasedir() + "/Assets/Plugins/Android");
					ProcessRunner processRunner = new ProcessRunner(getLog());
					processRunner.runProcess(null, "unzip", "-uo", zippedFile.getAbsolutePath(), "-d", AndroidPluginsFolder.getAbsolutePath());
				} catch (ArtifactResolutionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

	}

	private void copyArtifact(final Artifact artifact, final File resultFile) throws MojoFailureException {

		final File file = artifact.getFile();

		try {
			FileUtils.copyFileToDirectory(file, resultFile);
		} catch (final IOException e) {
			this.getLog().error("Problem copying dll " + file.getName() + " to " + resultFile.getAbsolutePath());
			this.getLog().error(e.getMessage());
			throw new MojoFailureException("Problem copying dll " + file.getName() + " to " + resultFile.getAbsolutePath());
		}

	}

}

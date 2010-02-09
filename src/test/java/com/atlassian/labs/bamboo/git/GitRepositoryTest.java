package com.atlassian.labs.bamboo.git;

import java.io.File;
import java.io.IOException;
import java.sql.Array;
import java.util.*;

import com.atlassian.labs.bamboo.git.model.CommitDescriptor;
import com.atlassian.labs.bamboo.git.model.HardCodedRepo;
import com.atlassian.labs.bamboo.git.model.Sha;
import edu.nyu.cs.javagit.api.JavaGitException;
import edu.nyu.cs.javagit.api.Ref;
import edu.nyu.cs.javagit.api.commands.*;
import edu.nyu.cs.javagit.client.GitResetResponseImpl;
import edu.nyu.cs.javagit.client.cli.IParser;
import edu.nyu.cs.javagit.client.cli.ProcessUtilities;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.After;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.repository.RepositoryException;

/**
 * @author Kristian Rosenvold
 */
public class GitRepositoryTest
{
    private static String getGitHubRepoUrl() {
        return "git://github.com/krosenvold/bgit-unittest.git";
    }


    @BeforeClass
    public static void getFromGitHub() throws IOException, JavaGitException {
        final File localRepo = getMasterRepoCheckoutDirectory();
        if ( !GitRepository.containsValidRepo(localRepo)){
            GitCloneOptions gitCloneOptions = new GitCloneOptions(false, false, true);
            GitRepository.clone( getGitHubRepoUrl(), localRepo, gitCloneOptions);
        }
    }


    @After
    public void deleteWorkingCopy() throws IOException, JavaGitException {
        deleteDir( getWorkingCopyDir());
    }


    @Test
    public void testClone() throws IOException, JavaGitException {
        GitRepository gitRepository = getGitRepository("feature1");
        File sourceDir = getFreshCheckoutDir();
        
        assertFalse( GitRepository.containsValidRepo( sourceDir));
        gitRepository.cloneOrFetch(sourceDir);
        assertTrue( GitRepository.containsValidRepo( sourceDir));

        assertEquals("Repository should be on feature1 branch", "feature1", gitRepository.gitStatus(sourceDir).getName());
    }

    @Test
    public void testCloneWithREquestedSha1() throws IOException, JavaGitException {
        GitRepository gitRepository = getGitRepository("feature1");
        File sourceDir = getFreshCheckoutDir();

        assertFalse( GitRepository.containsValidRepo( sourceDir));
        gitRepository.cloneOrFetch(sourceDir, HardCodedRepo.second_a55e.getSha().getSha());
        assertTrue( GitRepository.containsValidRepo( sourceDir));

        assertEquals("Repository should be on feature1 branch", "feature1", gitRepository.gitStatus(sourceDir).getName());

        assertEquals( gitRepository.gitLog(sourceDir,1 ).get(0).getSha(), HardCodedRepo.second_a55e.getSha().getSha());

        gitRepository.cloneOrFetch(sourceDir, HardCodedRepo.first.getSha().getSha());
        assertEquals( gitRepository.gitLog(sourceDir,1 ).get(0).getSha(), HardCodedRepo.first.getSha().getSha());
    }

    @Test
    public void testCloneThenMoveHeadThenFetch() throws IOException, JavaGitException {
        GitRepository gitRepository = getGitRepository("feature1");
        File sourceDir = getFreshCheckoutDir();

        assertFalse( GitRepository.containsValidRepo( sourceDir));
        gitRepository.cloneOrFetch(sourceDir);
        assertTrue( GitRepository.containsValidRepo( sourceDir));

        assertEquals("Repository should be on feature1 branch", "feature1", gitRepository.gitStatus(sourceDir).getName());


        GitResetOptions gitResetOptions = new GitResetOptions(GitResetOptions.ResetType.HARD, HardCodedRepo.second_a55e.getShaRef());
        GitReset.gitReset( sourceDir, gitResetOptions);

        gitRepository.cloneOrFetch(sourceDir);
        final Ref ref = gitRepository.gitStatus(sourceDir);
        assertEquals("Repository should be on feature1 branch", "feature1", ref.getName());
    }


    @Test
    public void testCloneThenRebaseLocal() throws IOException, JavaGitException {
        // Rebasing local branch should be more or less equivalent to rebasing remote branch.
        // Oops. Brain damaged git repo does not support rebase.


        GitRepository gitRepository = getGitRepository("feature1");
        File sourceDir = getFreshCheckoutDir();

        assertFalse( GitRepository.containsValidRepo( sourceDir));
        gitRepository.cloneOrFetch(sourceDir);
        assertTrue( GitRepository.containsValidRepo( sourceDir));

        IParser rebaseParser = new IParser(){
            public void parseLine(String line) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void processExitCode(int code) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public CommandResponse getResponse() throws JavaGitException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };

        List<String> commandLine = Arrays.asList( "git","rebase", "origin/feature2");

        CommandResponse rebase = ProcessUtilities.runCommand(sourceDir, commandLine, rebaseParser);


        // Todo: Need to assert the head points to a given commit.
        assertEquals("Repository should be on feature1 branch", "feature1", gitRepository.gitStatus(sourceDir).getName());

        gitRepository.cloneOrFetch(sourceDir);
        final Ref ref = gitRepository.gitStatus(sourceDir);
        assertEquals("Repository should be on feature1 branch", "feature1", ref.getName());
    }

    @Test
    public void testCloneThenSeveralBranchChanges() throws IOException, JavaGitException {
        GitRepository gitRepository = getGitRepository("feature1");
        File sourceDir = getFreshCheckoutDir();

        assertFalse( GitRepository.containsValidRepo( sourceDir));
        gitRepository.cloneOrFetch(sourceDir);
        assertTrue( GitRepository.containsValidRepo( sourceDir));

        assertEquals("Repository should be on feature1 branch", "feature1", gitRepository.gitStatus(sourceDir).getName());

        gitRepository.setRemoteBranch("aBranch");
        gitRepository.cloneOrFetch(sourceDir);
        assertEquals("Repository should be on feature1 branch", "aBranch", gitRepository.gitStatus(sourceDir).getName());

        // Switch back to
        gitRepository.setRemoteBranch("feature1");
        gitRepository.cloneOrFetch(sourceDir);
        assertEquals("Repository should be on feature1 branch", "feature1", gitRepository.gitStatus(sourceDir).getName());
    }

    private File getFreshCheckoutDir() {
        return getCheckoutDirectory(getFreshWorkingCopyDir());
    }


    @Test
    public void testCloneDefault() throws IOException, JavaGitException {
        GitRepository gitRepository = getGitRepository(null);
        File sourceDir = getFreshCheckoutDir();
        gitRepository.cloneOrFetch(sourceDir);
        assertEquals("featureDefault", gitRepository.gitStatus(sourceDir).getName());
    }

    @Test
    public void testIsOnBranch() throws IOException, JavaGitException {
        GitRepository gitRepository = getGitRepository( null);
        File sourceDir = getFreshCopyInCheckoutDir(gitRepository);

        assertTrue(gitRepository.isOnBranch(sourceDir, Ref.createBranchRef("featureDefault")));
        assertFalse(gitRepository.isOnBranch(sourceDir, Ref.createBranchRef("feature1")));
    }


    @Test
    public void testHistory() throws IOException, JavaGitException, RepositoryException {
        GitRepository gitRepository = getGitRepository( "featureDefault");
        File sourceDir = getFreshCopyInCheckoutDir(gitRepository);

        List<com.atlassian.bamboo.commit.Commit> results = new ArrayList<Commit>();

        gitRepository.detectCommitsForUrl(HardCodedRepo.first.getSha().getSha(), results, sourceDir, "UT-KEY");
        final CommitDescriptor commitDescriptor = HardCodedRepo.getBranchPointerFeatureDefault();


        System.out.println("commitDescriptor = " + commitDescriptor.collectNodesInRealGitLogOrder(HardCodedRepo.first.getSha()).toString());
        commitDescriptor.assertHistoryMatch( results, HardCodedRepo.first.getSha());
    }

    @Test
    public void testCollectChangesFromNonExistingSha1() throws IOException, JavaGitException, RepositoryException {
        GitRepository gitRepository = getGitRepository( "featureDefault");
        File sourceDir = getFreshCopyInCheckoutDir(gitRepository);

        List<com.atlassian.bamboo.commit.Commit> results = new ArrayList<Commit>();

        final String s = gitRepository.detectCommitsForUrl(HardCodedRepo.NONEXISTANT_SHA1.getSha().getSha(), results, sourceDir, "UT-KEY");
        assertEquals(HardCodedRepo.COMMIT_Merge_aBranch_featureDefault.getSha().getSha(), s );
        assertEquals(10, results.size());// This is a bit of a weird assert since I do not exactly understand why it gives me 10 items.

    }

    @Test
    public void testHistoryWithMergeCommit() throws IOException, JavaGitException, RepositoryException {
        GitRepository gitRepository = getGitRepository( "featureDefault");
        File sourceDir = getFreshCopyInCheckoutDir(gitRepository);

        List<com.atlassian.bamboo.commit.Commit> results = new ArrayList<Commit>();
        gitRepository.detectCommitsForUrl(HardCodedRepo.getFristCommitInBranch().getSha().getSha(), results, sourceDir, "UT-KEY");

        assertEquals( 7 , results.size());
    }

    @Test
    public void testHistoryFeature1() throws IOException, JavaGitException, RepositoryException {
        GitRepository gitRepository = getGitRepository( "feature1");
        File sourceDir = getFreshCopyInCheckoutDir(gitRepository);

        List<com.atlassian.bamboo.commit.Commit> results = new ArrayList<Commit>();
        final Sha untilSha = HardCodedRepo.getRootCommit().getSha();
        gitRepository.detectCommitsForUrl(untilSha.getSha(), results, sourceDir, "UT-KEY");

        HardCodedRepo.getFeature1Head().assertHistoryMatch( results, untilSha);
    }
    @Test
    public void testHistoryFeature2() throws IOException, JavaGitException, RepositoryException {
        GitRepository gitRepository = getGitRepository( "feature2");
        File sourceDir = getFreshCopyInCheckoutDir(gitRepository);

        List<com.atlassian.bamboo.commit.Commit> results = new ArrayList<Commit>();
        final Sha untilSha = HardCodedRepo.getRootCommit().getSha();
        gitRepository.detectCommitsForUrl(untilSha.getSha(), results, sourceDir, "UT-KEY");
        HardCodedRepo.getFeature2Head().assertHistoryMatch( results, untilSha);
    }

    @Test
    public void testNonLinearHistory() throws IOException, JavaGitException, RepositoryException {
        GitRepository gitRepository = getGitRepository( "featureDefault");
        File sourceDir = getFreshCopyInCheckoutDir(gitRepository);

        List<com.atlassian.bamboo.commit.Commit> results = new ArrayList<Commit>();

        gitRepository.detectCommitsForUrl(HardCodedRepo.COMMIT_fb65.getSha().getSha(), results, sourceDir, "UT-KEY");
        assertEquals(5, results.size());

        results = new ArrayList<Commit>();
        gitRepository.detectCommitsForUrl(HardCodedRepo.COMMIT_2d9b.getSha().getSha()  , results, sourceDir, "UT-KEY");
        HardCodedRepo.getBranchPointerFeatureDefault().assertHistoryMatch( results, HardCodedRepo.COMMIT_2d9b.getSha());


        // This tes fails because the test-data representation is still not correct.
        // Method collectNodesInRealGitLogOrder does not do it properly -- yet. Need to check both date and sha1
        results = new ArrayList<Commit>();
        gitRepository.detectCommitsForUrl(HardCodedRepo.COMMIT_3a45.getSha().getSha(), results, sourceDir, "UT-KEY");
        HardCodedRepo.getBranchPointerFeatureDefault().assertHistoryMatch( results, HardCodedRepo.COMMIT_3a45.getSha());
    }

    @Test
    public void testPluginUpgrade() throws IOException, JavaGitException, RepositoryException {
        GitRepository gitRepository = getGitRepository( "featureDefault");
        File sourceDir = getFreshCopyInCheckoutDir(gitRepository);

        List<com.atlassian.bamboo.commit.Commit> results = new ArrayList<Commit>();
        gitRepository.detectCommitsForUrl("Fri Oct 9 15:38:10 2009 +0200", results, sourceDir, "UT-KEY");

        assertEquals(8, results.size());
    }

    @Test
    public void testCloneNonExistingPrevious() throws IOException, JavaGitException, RepositoryException {
        GitRepository gitRepository = getGitRepository("feature1");
        File sourceDir = getFreshCheckoutDir();

        gitRepository.cloneOrFetch(sourceDir);
        assertTrue( GitRepository.containsValidRepo( sourceDir));

        List<com.atlassian.bamboo.commit.Commit> results = new ArrayList<Commit>();
        gitRepository.detectCommitsForUrl("e3bed58f697792d6e603c4c4a90cad1e9326a053", results, sourceDir, "UT-KEY");

    }

    @Test
    public void testgetSha1FromCommitDate() throws IOException, JavaGitException, RepositoryException {
        GitRepository gitRepository = getGitRepository("feature1");
        File sourceDir = getFreshCheckoutDir();
        gitRepository.cloneOrFetch(sourceDir);
        assertTrue( GitRepository.containsValidRepo( sourceDir));

        String commit = gitRepository.getSha1FromCommitDate("Fri Oct 9 15:38:10 2009 +0200", sourceDir);
        assertEquals( "a55e4702a0fdc210eaa17774dddc4890852396a7", commit);
        
        commit = gitRepository.getSha1FromCommitDate("Fri Oct 19 22:38:10 2009 +0200", sourceDir);// Fake
        assertEquals( "84965cc8dfc8af7fca02c78373413aceafc73c2f", commit);

        commit = gitRepository.getSha1FromCommitDate("YABBA", sourceDir);// Fake
        assertEquals( "84965cc8dfc8af7fca02c78373413aceafc73c2f", commit);

    }


    @Test
    public void testLastCheckedRevisionIsNull() throws IOException, JavaGitException, RepositoryException {
        GitRepository gitRepository = getGitRepository("featureDefault");
        File sourceDir = getFreshCopyInCheckoutDir(gitRepository);

        List<com.atlassian.bamboo.commit.Commit> results = new ArrayList<Commit>();
        gitRepository.detectCommitsForUrl(null, results, sourceDir, "UT-KEY");

        assertEquals(10, results.size());
    }

    private static File getMasterRepoWorkingDirectory() {
        File masterRepoDir = new File("masterRepo");
        ensureDirExists(masterRepoDir);
        return masterRepoDir;
    }

    private static File getWorkingCopyDir() {
        return new File("testRepo");
    }

    
    private static File getMasterRepoCheckoutDirectory() {
        return getMasterRepoCheckoutDirectory(getMasterRepoWorkingDirectory().getPath());
    }
    private static File getCWDRelativeMasterRepoCheckoutDirectory() {
        return getMasterRepoCheckoutDirectory(".." + File.separator + getMasterRepoWorkingDirectory().getPath());
    }
    private static File getMasterRepoCheckoutDirectory(String localPart) {
        try {
            final File file = new File(localPart + File.separator + GitRepository.getLocalCheckoutSubfolder());
            ensureDirExists( file);
            return file;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }


    private static File getCheckoutDirectory(File workingDirectory){
        try {
            return new File(workingDirectory.getCanonicalPath() + File.separator + GitRepository.getLocalCheckoutSubfolder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File getFreshWorkingCopyDir() {
        File workingCopyDir = new File("testRepo");
        if (workingCopyDir.exists()) deleteDir( workingCopyDir);
        ensureDirExists(workingCopyDir);
        return workingCopyDir;
    }

    private static void ensureDirExists(File workingCopyDir) {
        if (!workingCopyDir.exists()){
            //noinspection ResultOfMethodCallIgnored
            workingCopyDir.mkdir();
        }
    }


    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

     private File getFreshCopyInCheckoutDir(GitRepository gitRepository) throws IOException, JavaGitException {
        final File directory = getCheckoutDirectory(getFreshWorkingCopyDir());
        gitRepository.cloneOrFetch( directory);
        return directory;
    }


    private GitRepository getGitRepository(String remoteBranch) throws IOException {
        return new GitRepository(getCWDRelativeMasterRepoCheckoutDirectory().getPath(), remoteBranch);
    }


}

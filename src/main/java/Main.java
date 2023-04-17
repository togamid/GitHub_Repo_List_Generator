import org.kohsuke.github.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        GitHub github = GitHubBuilder.fromPropertyFile().build();
        String path = "list.txt";
        if(args.length == 1) {
            path = args[0] + "/" + path;
        }
        List<GHRepository> allRepos = getRepoList(github, 200);
        int totalRepos = allRepos.size();

        List<GHRepository> mvnRepos = allRepos.stream().filter(Main::isMavenRepo).collect(Collectors.toList());
        List<String> entries = mvnRepos.stream().map(Main::repoToCsvString).collect(Collectors.toList());
        List<String> totalEntries = allRepos.stream().map(Main::repoToCsvString).collect(Collectors.toList());
        saveList(entries, path);
        saveList(totalEntries, "totalList.txt");

        System.out.println("Total Repos: " + totalRepos +", davon Maven-Projekte: " + mvnRepos.size());
    }

    public static String repoToCsvString(GHRepository repository) {
        return repository.getFullName() +
                ";" + repository.getForksCount() +
                ";" + repository.getStargazersCount() +
                ";" + repository.getWatchersCount();
    }

    public static boolean isMavenRepo(GHRepository repository) {
        try {
            repository.getFileContent("pom.xml");
        } catch (GHFileNotFoundException e) {
            return false;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public static List<GHRepository> getRepoList(GitHub github, int maximumLength) {
        PagedSearchIterable<GHRepository> repos = github.searchRepositories()
                .language("Java")
                .created("<2019-01-01")
                .sort(GHRepositorySearchBuilder.Sort.UPDATED)
                .order(GHDirection.DESC)
                .list();
        PagedIterator<GHRepository> repoIterator = repos._iterator(100);
        int counter = 0;
        List<GHRepository> repoList = new ArrayList<>();
        while(repoIterator.hasNext() && counter < maximumLength){
            counter++;
            GHRepository repository = repoIterator.next();
            repoList.add(repository);
        }

        return repoList;
    }

    public static void saveList(List<String> list, String path) {
        String header = "Full Name; ForkCount; Stars; Watchers";
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(path));
            writer.write(header);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String entry:list) {
            try {
                writer.write(entry);
                writer.newLine();
            } catch (IOException e) {
                System.out.println("Could not write entry: " + entry);
            }
        }
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

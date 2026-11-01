import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * SkillTrack - Placement Readiness Engine
 *
 * OOP concepts used (for viva):
 *  1. Interface      : Scorable (a contract: anything that gives score points)
 *  2. Abstract class : Certification (shared fields, subclasses give the points)
 *  3. Inheritance    : TechCert and SoftSkillCert extend Certification
 *  4. Polymorphism   : Student calls calculateScoreContribution() on a
 *                      List<Scorable> without caring which class each item is
 *  5. Encapsulation  : private fields + getters + validation in constructors
 *  6. Enum           : Complexity (EASY / MEDIUM / HARD with fixed points)
 *  7. Exception      : InvalidInputException (custom checked exception)
 *  8. Collections    : ArrayList of certifications, projects, gaps
 *
 * Two ways to run:
 *   java SkillTrackEngine                  -> interactive console menu
 *   java SkillTrackEngine 2 1 1 1 0 50 30 2 -> JSON output (used by Flask app)
 *      order: techCerts softCerts easy medium hard leetcode gfg hackerrank
 */
public class SkillTrackEngine {

    // ------------------------------------------------------------------
    // Custom exception
    // ------------------------------------------------------------------
    static class InvalidInputException extends Exception {
        private static final long serialVersionUID = 1L;

        public InvalidInputException(String message) {
            super(message);
        }
    }

    // ------------------------------------------------------------------
    // Interface: anything that adds points to the readiness score
    // ------------------------------------------------------------------
    interface Scorable {
        double calculateScoreContribution();
    }

    // ------------------------------------------------------------------
    // Abstract class: common data for all certifications
    // ------------------------------------------------------------------
    abstract static class Certification implements Scorable {
        private final String title;
        private final String platform;
        private final String issueDate;

        public Certification(String title, String platform, String issueDate)
                throws InvalidInputException {
            if (title == null || title.trim().isEmpty()) {
                throw new InvalidInputException("Certification title cannot be empty.");
            }
            this.title = title.trim();
            this.platform = (platform == null) ? "" : platform.trim();
            this.issueDate = (issueDate == null) ? "" : issueDate.trim();
        }

        public String getTitle()     { return title; }
        public String getPlatform()  { return platform; }
        public String getIssueDate() { return issueDate; }

        // Each subclass says what kind of certification it is
        public abstract String getType();

        @Override
        public String toString() {
            return getType() + ": " + title + " (" + platform + ", " + issueDate + ")"
                    + " -> " + calculateScoreContribution() + " pts";
        }
    }

    static class TechCert extends Certification {
        private static final double POINTS = 15.0;

        public TechCert(String title, String platform, String issueDate)
                throws InvalidInputException {
            super(title, platform, issueDate);
        }

        @Override
        public double calculateScoreContribution() { return POINTS; }

        @Override
        public String getType() { return "Technical Certification"; }
    }

    static class SoftSkillCert extends Certification {
        private static final double POINTS = 5.0;

        public SoftSkillCert(String title, String platform, String issueDate)
                throws InvalidInputException {
            super(title, platform, issueDate);
        }

        @Override
        public double calculateScoreContribution() { return POINTS; }

        @Override
        public String getType() { return "Soft Skill Certification"; }
    }

    // ------------------------------------------------------------------
    // Enum: project difficulty with fixed points
    // ------------------------------------------------------------------
    enum Complexity {
        EASY(6.0), MEDIUM(12.0), HARD(20.0);

        private final double points;

        Complexity(double points) { this.points = points; }

        public double getPoints() { return points; }

        public static Complexity fromString(String text) throws InvalidInputException {
            try {
                return Complexity.valueOf(text.trim().toUpperCase());
            } catch (Exception e) {
                throw new InvalidInputException("Complexity must be easy, medium or hard.");
            }
        }
    }

    static class Project implements Scorable {
        private final String title;
        private final Complexity complexity;

        public Project(String title, Complexity complexity) throws InvalidInputException {
            if (title == null || title.trim().isEmpty()) {
                throw new InvalidInputException("Project title cannot be empty.");
            }
            if (complexity == null) {
                throw new InvalidInputException("Project complexity is required.");
            }
            this.title = title.trim();
            this.complexity = complexity;
        }

        public String getTitle()           { return title; }
        public Complexity getComplexity()  { return complexity; }

        @Override
        public double calculateScoreContribution() { return complexity.getPoints(); }

        @Override
        public String toString() {
            return "Project: " + title + " [" + complexity + "] -> "
                    + calculateScoreContribution() + " pts";
        }
    }

    // ------------------------------------------------------------------
    // Coding practice (LeetCode, GFG, HackerRank)
    // ------------------------------------------------------------------
    static class CodingProfile implements Scorable {
        private static final double LEETCODE_WEIGHT = 0.2;
        private static final double GFG_WEIGHT = 0.1;
        private static final double HACKERRANK_WEIGHT = 5.0;   // per star
        private static final double MAX_POINTS = 35.0;

        private final int leetcode;
        private final int gfg;
        private final int hackerrank;

        public CodingProfile(int leetcode, int gfg, int hackerrank)
                throws InvalidInputException {
            if (leetcode < 0 || gfg < 0 || hackerrank < 0) {
                throw new InvalidInputException("Coding counts cannot be negative.");
            }
            this.leetcode = leetcode;
            this.gfg = gfg;
            this.hackerrank = hackerrank;
        }

        public int getLeetcode()   { return leetcode; }
        public int getGfg()        { return gfg; }
        public int getHackerrank() { return hackerrank; }

        public int getTotalProblems() { return leetcode + gfg; }

        @Override
        public double calculateScoreContribution() {
            double score = (leetcode * LEETCODE_WEIGHT)
                         + (gfg * GFG_WEIGHT)
                         + (hackerrank * HACKERRANK_WEIGHT);
            return Math.min(score, MAX_POINTS);
        }
    }

    // ------------------------------------------------------------------
    // Student: owns everything and calculates the final score
    // ------------------------------------------------------------------
    static class Student {
        private static final double MAX_SCORE = 100.0;

        private final String regNo;
        private final String name;
        private final List<Certification> certifications = new ArrayList<>();
        private final List<Project> projects = new ArrayList<>();
        private CodingProfile codingProfile;

        public Student(String regNo, String name) throws InvalidInputException {
            if (regNo == null || regNo.trim().isEmpty()) {
                throw new InvalidInputException("Register number cannot be empty.");
            }
            this.regNo = regNo.trim();
            this.name = (name == null) ? "" : name.trim();
            this.codingProfile = new CodingProfile(0, 0, 0);
        }

        public String getRegNo() { return regNo; }
        public String getName()  { return name; }

        public void addCertification(Certification c) { certifications.add(c); }
        public void addProject(Project p)             { projects.add(p); }
        public void setCodingProfile(CodingProfile c) { this.codingProfile = c; }

        public int countTechCerts() {
            int count = 0;
            for (Certification c : certifications) {
                if (c instanceof TechCert) count++;
            }
            return count;
        }

        public int getProjectCount() { return projects.size(); }

        // POLYMORPHISM: one list, different classes, same method call
        public double calculateReadinessScore() {
            List<Scorable> items = new ArrayList<>();
            items.addAll(certifications);
            items.addAll(projects);
            items.add(codingProfile);

            double total = 0;
            for (Scorable item : items) {
                total += item.calculateScoreContribution();
            }
            return Math.min(total, MAX_SCORE);
        }

        public List<String> generateGaps() {
            List<String> gaps = new ArrayList<>();
            if (countTechCerts() < 2) {
                gaps.add("Low Tech Certifications: Complete at least 2 technical certs.");
            }
            if (getProjectCount() < 2) {
                gaps.add("Insufficient Projects: Add at least 2 projects.");
            }
            if (codingProfile.getTotalProblems() < 100) {
                gaps.add("Weak Problem Solving: Solve 100+ problems on LeetCode/GFG.");
            }
            if (gaps.isEmpty()) {
                gaps.add("Profile looks solid! Keep practicing.");
            }
            return gaps;
        }

        public void printReport() {
            System.out.println("\n===== SkillTrack Report =====");
            System.out.println("Reg No : " + regNo);
            if (!name.isEmpty()) System.out.println("Name   : " + name);

            System.out.println("\nCertifications:");
            if (certifications.isEmpty()) System.out.println("  (none)");
            for (Certification c : certifications) System.out.println("  - " + c);

            System.out.println("\nProjects:");
            if (projects.isEmpty()) System.out.println("  (none)");
            for (Project p : projects) System.out.println("  - " + p);

            System.out.println("\nCoding: LeetCode=" + codingProfile.getLeetcode()
                    + ", GFG=" + codingProfile.getGfg()
                    + ", HackerRank stars=" + codingProfile.getHackerrank()
                    + " -> " + codingProfile.calculateScoreContribution() + " pts");

            System.out.println("\nReadiness Score: "
                    + String.format(Locale.US, "%.1f", calculateReadinessScore()) + " / 100");
            System.out.println("\nWhat to fix:");
            for (String g : generateGaps()) System.out.println("  * " + g);
            System.out.println("=============================\n");
        }
    }

    // ------------------------------------------------------------------
    // JSON mode (called by Flask)
    // ------------------------------------------------------------------
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String errorJson(String message) {
        return "{\"error\": \"" + escapeJson(message) + "\"}";
    }

    private static void runJsonMode(String[] args) {
        if (args.length < 8) {
            System.out.println(errorJson("Invalid arguments supplied to Java Engine"));
            return;
        }
        try {
            int techCerts      = Integer.parseInt(args[0]);
            int softCerts      = Integer.parseInt(args[1]);
            int easyProjects   = Integer.parseInt(args[2]);
            int mediumProjects = Integer.parseInt(args[3]);
            int hardProjects   = Integer.parseInt(args[4]);
            int leetcode       = Integer.parseInt(args[5]);
            int gfg            = Integer.parseInt(args[6]);
            int hackerrank     = Integer.parseInt(args[7]);

            if (techCerts < 0 || softCerts < 0 || easyProjects < 0
                    || mediumProjects < 0 || hardProjects < 0) {
                throw new InvalidInputException("Counts cannot be negative.");
            }

            Student student = new Student("web-user", "");

            for (int i = 1; i <= techCerts; i++)
                student.addCertification(new TechCert("Tech Cert " + i, "-", "-"));
            for (int i = 1; i <= softCerts; i++)
                student.addCertification(new SoftSkillCert("Soft Cert " + i, "-", "-"));
            for (int i = 1; i <= easyProjects; i++)
                student.addProject(new Project("Easy Project " + i, Complexity.EASY));
            for (int i = 1; i <= mediumProjects; i++)
                student.addProject(new Project("Medium Project " + i, Complexity.MEDIUM));
            for (int i = 1; i <= hardProjects; i++)
                student.addProject(new Project("Hard Project " + i, Complexity.HARD));

            student.setCodingProfile(new CodingProfile(leetcode, gfg, hackerrank));

            List<String> gaps = student.generateGaps();
            StringBuilder json = new StringBuilder();
            json.append("{\"readinessScore\": ")
                .append(String.format(Locale.US, "%.1f", student.calculateReadinessScore()))
                .append(", \"gaps\": [");
            for (int i = 0; i < gaps.size(); i++) {
                json.append("\"").append(escapeJson(gaps.get(i))).append("\"");
                if (i < gaps.size() - 1) json.append(",");
            }
            json.append("]}");
            System.out.println(json);

        } catch (NumberFormatException e) {
            System.out.println(errorJson("All inputs must be whole numbers."));
        } catch (InvalidInputException e) {
            System.out.println(errorJson(e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // Console mode (for lab exam demo)
    // ------------------------------------------------------------------
    private static int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static String readText(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
    }

    private static void runConsoleMode() {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== SkillTrack: Placement Readiness ===");

        Student student;
        while (true) {
            try {
                String reg = readText(sc, "Register number: ");
                String name = readText(sc, "Name: ");
                student = new Student(reg, name);
                break;
            } catch (InvalidInputException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        boolean running = true;
        while (running) {
            System.out.println("1. Add technical certification");
            System.out.println("2. Add soft skill certification");
            System.out.println("3. Add project");
            System.out.println("4. Set coding profile");
            System.out.println("5. Show report");
            System.out.println("0. Exit");
            int choice = readInt(sc, "Choose: ");

            try {
                switch (choice) {
                    case 1:
                    case 2: {
                        String title = readText(sc, "Title: ");
                        String platform = readText(sc, "Platform: ");
                        String date = readText(sc, "Issue date: ");
                        if (choice == 1) student.addCertification(new TechCert(title, platform, date));
                        else student.addCertification(new SoftSkillCert(title, platform, date));
                        System.out.println("Certification added.\n");
                        break;
                    }
                    case 3: {
                        String title = readText(sc, "Project title: ");
                        Complexity cx = Complexity.fromString(readText(sc, "Complexity (easy/medium/hard): "));
                        student.addProject(new Project(title, cx));
                        System.out.println("Project added.\n");
                        break;
                    }
                    case 4: {
                        int lc = readInt(sc, "LeetCode problems solved: ");
                        int gf = readInt(sc, "GFG problems solved: ");
                        int hr = readInt(sc, "HackerRank stars: ");
                        student.setCodingProfile(new CodingProfile(lc, gf, hr));
                        System.out.println("Coding profile saved.\n");
                        break;
                    }
                    case 5:
                        student.printReport();
                        break;
                    case 0:
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice.\n");
                }
            } catch (InvalidInputException e) {
                System.out.println("Error: " + e.getMessage() + "\n");
            }
        }
        System.out.println("Goodbye!");
    }

    // ------------------------------------------------------------------
    public static void main(String[] args) {
        if (args.length == 0) {
            runConsoleMode();
        } else {
            runJsonMode(args);
        }
    }
}

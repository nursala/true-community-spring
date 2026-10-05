package com.truecommunity.config;

import com.truecommunity.model.Comment;
import com.truecommunity.model.CommentVote;
import com.truecommunity.model.CommentVoteType;
import com.truecommunity.model.EditorDecision;
import com.truecommunity.model.Role;
import com.truecommunity.model.Speaker;
import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.Topic;
import com.truecommunity.model.User;
import com.truecommunity.model.UserActivityLog;
import com.truecommunity.model.UserActivityType;
import com.truecommunity.model.Verdict;
import com.truecommunity.model.Vote;
import com.truecommunity.repo.CommentRepository;
import com.truecommunity.repo.CommentVoteRepository;
import com.truecommunity.repo.EditorDecisionRepository;
import com.truecommunity.repo.SpeakerRepository;
import com.truecommunity.repo.StatementRepository;
import com.truecommunity.repo.TopicRepository;
import com.truecommunity.repo.UserActivityLogRepository;
import com.truecommunity.repo.UserRepository;
import com.truecommunity.repo.VoteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seeds deterministic demo users, topics, speakers, statements, votes,
 * comments, decisions, and activity logs when the database is empty. The
 * initializer is idempotent for the seeded data it owns and keeps demo content
 * stable by using a fixed random seed.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_EDITOR_USERNAME = "editor";
    private static final String DEFAULT_EDITOR_EMAIL = "editor@truecommunity.local";
    private static final String DEFAULT_EDITOR_PASSWORD = "editor123";
    private static final String DEFAULT_USER_PASSWORD = "password123";
    private static final long SEED = 20_260_620L;

    private static final List<String> SYSTEM_TOPICS = List.of(
            "חוץ", "ביטחון", "כלכלה", "משפט", "פוליטיקה",
            "חברה", "תקשורת", "בריאות", "חינוך", "סביבה"
    );

    private static final List<Verdict> VERDICTS = List.of(
            Verdict.TRUE,
            Verdict.FALSE,
            Verdict.MISLEADING,
            Verdict.MISSING_CONTEXT,
            Verdict.SATIRE
    );

    private static final List<String> VOTE_EXPLANATIONS = List.of(
            "מצאתי נתון רשמי שמחזק את הטענה.",
            "המספר המוצג לא תואם לדוח הציבורי האחרון.",
            "יש כאן חלק נכון, אבל חסרים נתונים שמשנים את התמונה.",
            "המקור שצוטט אינו תומך במסקנה הזו.",
            "כדאי להשוות לנתונים מעודכנים לפני קביעה סופית.",
            "הטענה מתייחסת לתקופה אחרת מזו שהוזכרה בכותרת."
    );

    private static final List<String> COMMENT_TEXTS = List.of(
            "בדקתי את המקור המקורי ונראה שהניסוח חלקי.",
            "הקישור למקור מעלה נקודה חשובה, אבל לא את כל הנתונים.",
            "לדעתי יש להבחין בין מוצע ארצי לבין הנתון בפועל.",
            "ההקשר הרחב חסר כאן וזה עלול להטעות את הקוראים.",
            "מסכים עם ההסבר וממליץ להוסיף קישור לדוח המלא.",
            "הטענה נראית סבירה, אבל האחוזים שצוינו דורשים אימות.",
            "ניתן למצוא את הנתונים האלה במספר מקורות בלתי תלויים."
    );

    private static final List<DemoSpeaker> DEMO_SPEAKERS = List.of(
            new DemoSpeaker("https://example.com/speakers/speaker-01", "אורי כהן", "פרשן פוליטי", "https://i.pravatar.cc/150?img=1"),
            new DemoSpeaker("https://example.com/speakers/speaker-02", "נועה לוי", "כלכלן", "https://i.pravatar.cc/150?img=2"),
            new DemoSpeaker("https://example.com/speakers/speaker-03", "איתי מזרחי", "עיתונאי", "https://i.pravatar.cc/150?img=3"),
            new DemoSpeaker("https://example.com/speakers/speaker-04", "מאיה פרץ", "חוקר מדיניות", "https://i.pravatar.cc/150?img=4"),
            new DemoSpeaker("https://example.com/speakers/speaker-05", "יונתן ביטון", "משפיען רשת", "https://i.pravatar.cc/150?img=5"),
            new DemoSpeaker("https://example.com/speakers/speaker-06", "שירה אברהם", "כתב חדשות", "https://i.pravatar.cc/150?img=6"),
            new DemoSpeaker("https://example.com/speakers/speaker-07", "עומר פרידמן", "ראש מכון מחקר", "https://i.pravatar.cc/150?img=7"),
            new DemoSpeaker("https://example.com/speakers/speaker-08", "טל שפירא", "פובליציסט", "https://i.pravatar.cc/150?img=8"),
            new DemoSpeaker("https://example.com/speakers/speaker-09", "רון אזולאי", "מרצה", "https://i.pravatar.cc/150?img=9"),
            new DemoSpeaker("https://example.com/speakers/speaker-10", "ליאור דהן", "יועץ תקשורת", "https://i.pravatar.cc/150?img=10"),
            new DemoSpeaker("https://example.com/speakers/speaker-11", "דנה גבאי", "פרשן פוליטי", "https://i.pravatar.cc/150?img=11"),
            new DemoSpeaker("https://example.com/speakers/speaker-12", "גיא רוזן", "כלכלן", "https://i.pravatar.cc/150?img=12"),
            new DemoSpeaker("https://example.com/speakers/speaker-13", "יעל ברק", "עיתונאי", "https://i.pravatar.cc/150?img=13"),
            new DemoSpeaker("https://example.com/speakers/speaker-14", "אלון הראל", "חוקר מדיניות", "https://i.pravatar.cc/150?img=14"),
            new DemoSpeaker("https://example.com/speakers/speaker-15", "הדר סגל", "משפיען רשת", "https://i.pravatar.cc/150?img=15"),
            new DemoSpeaker("https://example.com/speakers/speaker-16", "ניר נחמיאס", "כתב חדשות", "https://i.pravatar.cc/150?img=16"),
            new DemoSpeaker("https://example.com/speakers/speaker-17", "רותם בן דוד", "ראש מכון מחקר", "https://i.pravatar.cc/150?img=17"),
            new DemoSpeaker("https://example.com/speakers/speaker-18", "עדן אשכנזי", "פובליציסט", "https://i.pravatar.cc/150?img=18"),
            new DemoSpeaker("https://example.com/speakers/speaker-19", "אסף וקנין", "מרצה", "https://i.pravatar.cc/150?img=19"),
            new DemoSpeaker("https://example.com/speakers/speaker-20", "מיכל גולן", "יועץ תקשורת", "https://i.pravatar.cc/150?img=20"),
            new DemoSpeaker("https://example.com/speakers/speaker-21", "דניאל אלבז", "פרשן פוליטי", "https://i.pravatar.cc/150?img=21"),
            new DemoSpeaker("https://example.com/speakers/speaker-22", "ענבר ניסים", "כלכלן", "https://i.pravatar.cc/150?img=22"),
            new DemoSpeaker("https://example.com/speakers/speaker-23", "יובל טל", "עיתונאי", "https://i.pravatar.cc/150?img=23"),
            new DemoSpeaker("https://example.com/speakers/speaker-24", "שני ברקוביץ", "חוקר מדיניות", "https://i.pravatar.cc/150?img=24"),
            new DemoSpeaker("https://example.com/speakers/speaker-25", "אמיר מלכה", "משפיען רשת", "https://i.pravatar.cc/150?img=25"),
            new DemoSpeaker("https://example.com/speakers/speaker-26", "קרן עמר", "כתב חדשות", "https://i.pravatar.cc/150?img=26"),
            new DemoSpeaker("https://example.com/speakers/speaker-27", "בר שרון", "ראש מכון מחקר", "https://i.pravatar.cc/150?img=27"),
            new DemoSpeaker("https://example.com/speakers/speaker-28", "נטע גל", "פובליציסט", "https://i.pravatar.cc/150?img=28"),
            new DemoSpeaker("https://example.com/speakers/speaker-29", "עידו נדב", "מרצה", "https://i.pravatar.cc/150?img=29"),
            new DemoSpeaker("https://example.com/speakers/speaker-30", "לירון רון", "יועץ תקשורת", "https://i.pravatar.cc/150?img=30"),
            new DemoSpeaker("https://example.com/speakers/speaker-31", "שחר זיו", "פרשן פוליטי", "https://i.pravatar.cc/150?img=31")
    );
    private static final List<DemoStatement> DEMO_STATEMENTS = List.of(
            new DemoStatement("שיעור האינפלציה ברבעון האחרון ירד בחצי אחוז.", /* Bodkim */ 0, "https://www.gov.il/", List.of("כלכלה"), StatementStatus.PENDING, null, null),
            new DemoStatement("תקציב הביטחון לשנה הקרובה גדל בשנים עשר אחוזים.", /* Gil Feldman */ 1, "https://main.knesset.gov.il/", List.of("ביטחון", "כלכלה"), StatementStatus.PENDING, null, null),
            new DemoStatement("שיעור האבטלה ירד לשפל של 3.2 אחוזים בחודש האחרון.", /* Stop Hypocrisy */ 2, "https://www.cbs.gov.il/", List.of("כלכלה", "חברה"), StatementStatus.PENDING, null, null),
            new DemoStatement("מחירי הדיור במרכז הארץ ירדו בשני אחוזים בשנה החולפת.", /* Habodkim */ 3, "https://www.gov.il/", List.of("כלכלה", "חברה"), StatementStatus.PENDING, null, null),
            new DemoStatement("זמני המתנה לתורים במרפאות הציבוריות קצרו בעשרים אחוזים.", /* The Whistle */ 4, "https://www.gov.il/", List.of("בריאות", "חברה"), StatementStatus.PENDING, null, null),
            new DemoStatement("מספר נסיעות האוטוובוסים באצורי הפריפריה גדל בעשרה אחוזים.", /* Haoketz */ 5, "https://www.gov.il/", List.of("חברה", "סביבה"), StatementStatus.PENDING, null, null),
            new DemoStatement("שיעור הנשירה מבתי הספר היסודיים הצטמצם בשנה החולפת.", /* The Seventh Eye */ 6, "https://www.gov.il/", List.of("חינוך", "חברה"), StatementStatus.PENDING, null, null),
            new DemoStatement("כמות המים המותפלים גדלה בבת אחת בעשרים אחוזים.", /* The Liar */ 7, "https://www.gov.il/", List.of("סביבה", "כלכלה"), StatementStatus.PENDING, null, null),
            new DemoStatement("נחתם הסכם סחר חופשי חדש עם מדינה באירופה.", /* News From A Year Ago */ 8, "https://www.gov.il/", List.of("חוץ", "כלכלה"), StatementStatus.UNDER_REVIEW, null, null),
            new DemoStatement("ערוץ תקשורת ציבורי העלה את שיעור הצפייה לצעירים.", /* Medaykim */ 9, "https://www.gov.il/", List.of("תקשורת", "חברה"), StatementStatus.UNDER_REVIEW, null, null),
            new DemoStatement("הרפורמה המשפטית לא השפיעה על דירוג האשראי של המדינה.", /* Fighting Fake Incitement */ 10, "https://www.gov.il/", List.of("משפט", "כלכלה"), StatementStatus.DECIDED, Verdict.MISLEADING, "הטענה הציגה קשר חלקי בלבד ולא תמונה מלאה."),
            new DemoStatement("הכנסת אישרה חוק שמבטל לחלוטין את שיעורי המיסוי לעסקים קטנים.", /* Fake Reporter */ 11, "https://main.knesset.gov.il/", List.of("משפט", "פוליטיקה"), StatementStatus.DECIDED, Verdict.FALSE, "החוק לא ביטל את המיסוי אלא שינה את תנאי הזכאות.")
    );

    private final UserRepository userRepository;
    private final StatementRepository statementRepository;
    private final SpeakerRepository speakerRepository;
    private final TopicRepository topicRepository;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final EditorDecisionRepository editorDecisionRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           StatementRepository statementRepository,
                           SpeakerRepository speakerRepository,
                           TopicRepository topicRepository,
                           VoteRepository voteRepository,
                           CommentRepository commentRepository,
                           CommentVoteRepository commentVoteRepository,
                           EditorDecisionRepository editorDecisionRepository,
                           UserActivityLogRepository userActivityLogRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.statementRepository = statementRepository;
        this.speakerRepository = speakerRepository;
        this.topicRepository = topicRepository;
        this.voteRepository = voteRepository;
        this.commentRepository = commentRepository;
        this.commentVoteRepository = commentVoteRepository;
        this.editorDecisionRepository = editorDecisionRepository;
        this.userActivityLogRepository = userActivityLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Populates the demo database in one transaction during application startup.
     *
     * @param args command-line arguments supplied by Spring Boot
     */
    @Override
    @Transactional
    public void run(String... args) {
        Random random = new Random(SEED);
        List<Topic> topics = seedTopics();
        List<Speaker> speakers = seedSpeakers();
        User editor = seedDefaultEditor();
        List<User> users = seedDemoUsers(random);
        List<Statement> statements = seedStatements(topics, speakers, editor);

        for (Statement statement : statements) {
            seedVotes(statement, users, topics, random);
            seedComments(statement, users, random);
        }
    }

    private List<Topic> seedTopics() {
        return SYSTEM_TOPICS.stream()
                .map(name -> topicRepository.findByName(name)
                        .orElseGet(() -> topicRepository.save(Topic.create(name))))
                .toList();
    }


    private List<Speaker> seedSpeakers() {
        List<Speaker> speakers = new ArrayList<>();
        for (DemoSpeaker seed : DEMO_SPEAKERS) {
            speakers.add(speakerRepository.findByNameIgnoreCase(seed.name())
                    .map(existing -> updateSpeaker(existing, seed))
                    .orElseGet(() -> speakerRepository.save(Speaker.create(seed.name(), seed.organization(), seed.profileUrl(), seed.imageUrl()))));
        }
        return speakers;
    }

    private Speaker updateSpeaker(Speaker speaker, DemoSpeaker seed) {
        boolean changed = false;
        if (!seed.organization().equals(speaker.getRole())) {
            speaker.setRole(seed.organization());
            changed = true;
        }
        if (!seed.profileUrl().equals(speaker.getProfileUrl())) {
            speaker.setProfileUrl(seed.profileUrl());
            changed = true;
        }
        if (!seed.imageUrl().equals(speaker.getImageUrl())) {
            speaker.setImageUrl(seed.imageUrl());
            changed = true;
        }
        return changed ? speakerRepository.save(speaker) : speaker;
    }

    private User seedDefaultEditor() {
        User editor = userRepository.findByEmail(DEFAULT_EDITOR_EMAIL)
                .orElseGet(() -> {
                    User created = User.createRegularUser(
                            DEFAULT_EDITOR_USERNAME,
                            DEFAULT_EDITOR_EMAIL,
                            passwordEncoder.encode(DEFAULT_EDITOR_PASSWORD)
                    );
                    created.setRole(Role.EDITOR);
                    created.setTrustScore(5.0);
                    return userRepository.save(created);
                });
        // Ensure the primary editor is always protected, even on existing databases.
        if (!editor.isSeededAdmin()) {
            editor.markAsSeededAdmin();
            editor = userRepository.save(editor);
        }
        return editor;
    }

    private List<User> seedDemoUsers(Random random) {
        List<User> users = new ArrayList<>();
        for (int index = 1; index <= 30; index++) {
            int userNumber = index;
            String email = "user" + userNumber + "@example.com";
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> createDemoUser(userNumber, email, random));
            users.add(user);
        }
        return users;
    }

    private User createDemoUser(int index, String email, Random random) {
        User user = User.createRegularUser(
                "user_" + index,
                email,
                passwordEncoder.encode(DEFAULT_USER_PASSWORD)
        );
        double trustScore = 0.5 + random.nextDouble() * 4.5;
        user.setTrustScore(Math.round(trustScore * 100.0) / 100.0);
        return userRepository.save(user);
    }

    private List<Statement> seedStatements(List<Topic> topics, List<Speaker> speakers, User editor) {
        List<Statement> persistedStatements = statementRepository.findAll();
        List<Statement> statements = new ArrayList<>();

        for (DemoStatement seed : DEMO_STATEMENTS) {
            Speaker speaker = speakerFor(seed, speakers);
            Statement statement = persistedStatements.stream()
                    .filter(existing -> existing.getText().equals(seed.text()))
                    .findFirst()
                    .orElseGet(() -> createStatement(seed, speaker, topics));
            attachSpeaker(statement, speaker);
            applyDemoState(statement, seed, editor);
            statements.add(statement);
        }
        return statements;
    }

    private Speaker speakerFor(DemoStatement seed, List<Speaker> speakers) {
        if (seed.speakerIndex() < 0 || seed.speakerIndex() >= speakers.size()) {
            throw new IllegalStateException("Demo statement references missing speaker index: " + seed.speakerIndex());
        }
        Speaker speaker = speakers.get(seed.speakerIndex());
        if (speaker.getRole() == null || speaker.getImageUrl() == null || speaker.getProfileUrl() == null) {
            throw new IllegalStateException("Demo statement references incomplete speaker index: " + seed.speakerIndex());
        }
        return speaker;
    }

    private Statement createStatement(DemoStatement seed, Speaker speaker, List<Topic> topics) {
        // Pending statements start unclassified; the editor assigns topics when
        // publishing the verdict. Statements seeded as under-review/decided already
        // carry their editorial classification.
        Set<Topic> statementTopics = seed.status() == StatementStatus.PENDING
                ? Set.of()
                : topics.stream()
                        .filter(topic -> seed.topicNames().contains(topic.getName()))
                        .collect(Collectors.toSet());
        Statement statement = Statement.createPending(
                seed.text(),
                speaker,
                seed.sourceUrl(),
                statementTopics
        );
        return statementRepository.save(statement);
    }


    private void attachSpeaker(Statement statement, Speaker speaker) {
        if (speaker != null && (statement.getSpeaker() == null || !speaker.getId().equals(statement.getSpeaker().getId()))) {
            statement.setSpeaker(speaker);
            statementRepository.save(statement);
        }
    }

    private void applyDemoState(Statement statement, DemoStatement seed, User editor) {
        if (seed.status() == StatementStatus.UNDER_REVIEW && statement.getStatus() != StatementStatus.DECIDED) {
            if (!statement.isClaimed()) {
                statement.claim(editor);
                statementRepository.save(statement);
            }
            seedEditorActivity(statement, editor, UserActivityType.CLAIMED, statement.getId());
            return;
        }

        if (seed.status() == StatementStatus.DECIDED) {
            if (statement.getStatus() != StatementStatus.DECIDED) {
                statement.markDecided();
                statementRepository.save(statement);
            }
            EditorDecision decision = editorDecisionRepository.findByStatement(statement)
                    .orElseGet(() -> editorDecisionRepository.save(EditorDecision.create(
                            statement,
                            editor,
                            seed.finalVerdict(),
                            seed.editorNotes()
                    )));
            seedEditorActivity(statement, editor, UserActivityType.DECIDED, decision.getId());
        }
    }

    private void seedEditorActivity(Statement statement, User editor, UserActivityType actionType, Long targetId) {
        if (!userActivityLogRepository.existsByStatementAndActionTypeAndTargetId(statement, actionType, targetId)) {
            userActivityLogRepository.save(UserActivityLog.create(editor, actionType, statement, targetId));
        }
    }
    private void seedVotes(Statement statement, List<User> users, List<Topic> topics, Random random) {
        List<User> candidates = new ArrayList<>(users);
        Collections.shuffle(candidates, random);
        int targetVotes = 12 + random.nextInt(10);
        int createdVotes = 0;

        for (User user : candidates) {
            if (createdVotes == targetVotes) {
                break;
            }
            if (voteRepository.existsByUserAndStatement(user, statement)) {
                continue;
            }

            Vote vote = Vote.create(
                    user,
                    statement,
                    VERDICTS.get(random.nextInt(VERDICTS.size())),
                    VOTE_EXPLANATIONS.get(random.nextInt(VOTE_EXPLANATIONS.size()))
            );
            // Crowdsourced classification: each demo vote suggests a random topic.
            vote.setSuggestedTopic(topics.get(random.nextInt(topics.size())));
            voteRepository.save(vote);
            createdVotes++;
        }
    }

    private void seedComments(Statement statement, List<User> users, Random random) {
        List<Comment> comments = new ArrayList<>(commentRepository.findByStatementOrderByCreatedAtDesc(statement));
        int targetComments = 6 + random.nextInt(6);

        while (comments.size() < targetComments) {
            User author = users.get(random.nextInt(users.size()));
            Comment comment = Comment.create(
                    author,
                    statement,
                    COMMENT_TEXTS.get(random.nextInt(COMMENT_TEXTS.size()))
            );
            comments.add(commentRepository.save(comment));
        }

        for (Comment comment : comments) {
            seedCommentVotes(comment, users, random);
        }
    }

    private void seedCommentVotes(Comment comment, List<User> users, Random random) {
        List<User> candidates = new ArrayList<>(users);
        Collections.shuffle(candidates, random);
        int targetInteractions = 4 + random.nextInt(9);
        int createdInteractions = 0;

        for (User user : candidates) {
            if (createdInteractions == targetInteractions) {
                break;
            }
            if (commentVoteRepository.existsByCommentAndUser(comment, user)) {
                continue;
            }

            CommentVoteType voteType = random.nextDouble() < 0.7
                    ? CommentVoteType.UPVOTE
                    : CommentVoteType.DOWNVOTE;
            commentVoteRepository.save(CommentVote.create(comment, user, voteType));
            createdInteractions++;
        }
    }

    private record DemoStatement(String text,
                                 int speakerIndex,
                                 String sourceUrl,
                                 List<String> topicNames,
                                 StatementStatus status,
                                 Verdict finalVerdict,
                                 String editorNotes) {
    }

    private record DemoSpeaker(String profileUrl,
                               String name,
                               String organization,
                               String imageUrl) {
    }
}

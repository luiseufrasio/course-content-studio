package ai.agentic.samples.course.model;

import java.util.List;

/**
 * The student-facing lesson produced by {@code PublishAgent} from an approved
 * packet: the same intro/quiz/conclusion, plus generated "what you'll learn"
 * objectives and a publication timestamp.
 */
public class PublishedLesson {

    private String subject;
    private String chapterTitle;
    private List<String> objectives;
    private String intro;
    private Quiz quiz;
    private String conclusion;
    private String publishedAt;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public List<String> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<String> objectives) {
        this.objectives = objectives;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }
}

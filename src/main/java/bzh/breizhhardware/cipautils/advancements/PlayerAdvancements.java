
package bzh.breizhhardware.cipautils.advancements;

public class PlayerAdvancements {
    private int totalAdvancements;
    private int completedAdvancements;

    private int totalTasks;
    private int completedTasks;
    private int totalGoals;
    private int completedGoals;
    private int totalChallenges;
    private int completedChallenges;

    public PlayerAdvancements(int totalAdvancements, int completedAdvancements) {
        this.totalAdvancements = totalAdvancements;
        this.completedAdvancements = completedAdvancements;
    }

    public int getTotalAdvancements() {
        return totalAdvancements;
    }

    public void setTotalAdvancements(int totalAdvancements) {
        this.totalAdvancements = totalAdvancements;
    }

    public int getCompletedAdvancements() {
        return completedAdvancements;
    }

    public void setCompletedAdvancements(int completedAdvancements) {
        this.completedAdvancements = completedAdvancements;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getTotalGoals() {
        return totalGoals;
    }

    public void setTotalGoals(int totalGoals) {
        this.totalGoals = totalGoals;
    }

    public int getCompletedGoals() {
        return completedGoals;
    }

    public void setCompletedGoals(int completedGoals) {
        this.completedGoals = completedGoals;
    }

    public int getTotalChallenges() {
        return totalChallenges;
    }

    public void setTotalChallenges(int totalChallenges) {
        this.totalChallenges = totalChallenges;
    }

    public int getCompletedChallenges() {
        return completedChallenges;
    }

    public void setCompletedChallenges(int completedChallenges) {
        this.completedChallenges = completedChallenges;
    }
}
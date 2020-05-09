package jade.sfu.walkinggroup.dataobjects;

import android.graphics.Color;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom class that your group can change the format of in (almost) any way you like
 * to encode the rewards that this user has earned.
 * <p>
 * This class gets serialized/deserialized as part of a User object. Server stores it as
 * a JSON string, so it has no direct knowledge of what it contains.
 * (Rewards may not be used during first project iteration or two)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EarnedRewards {
    private String title = "Dragon slayer";
    private List<File> possibleBackgroundFiles = new ArrayList<>();
    private Integer selectedBackground = 1;
    private Integer titleColor = Color.BLUE;
    private Boolean hasColorScheme1 = false;
    private Boolean hasColorScheme2 = false;
    private Boolean hasMapIcon = false;

    @Override
    public String toString() {
        return "EarnedRewards{" +
                "hasColorScheme1=" + hasColorScheme1 +
                ", hasColorScheme2=" + hasColorScheme2 +
                ", hasMapIcon=" + hasMapIcon +
                '}';
    }

    public Boolean getHasColorScheme1() {
        return hasColorScheme1;
    }

    public void setHasColorScheme1(Boolean hasColorScheme1) {
        this.hasColorScheme1 = hasColorScheme1;
    }

    public Boolean getHasColorScheme2() {
        return hasColorScheme2;
    }

    public void setHasColorScheme2(Boolean hasColorScheme2) {
        this.hasColorScheme2 = hasColorScheme2;
    }

    public Boolean getHasMapIcon() {
        return hasMapIcon;
    }

    public void setHasMapIcon(Boolean hasMapIcon) {
        this.hasMapIcon = hasMapIcon;
    }



    // Needed for JSON deserialization
    public EarnedRewards() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<File> getPossibleBackgroundFiles() {
        return possibleBackgroundFiles;
    }

    public void setPossibleBackgroundFiles(List<File> possibleBackgroundFiles) {
        this.possibleBackgroundFiles = possibleBackgroundFiles;
    }

    public int getSelectedBackground() {
        return selectedBackground;
    }

    public void setSelectedBackground(int selectedBackground) {
        this.selectedBackground = selectedBackground;
    }

    public int getTitleColor() {
        return titleColor;
    }

    public void setTitleColor(int titleColor) {
        this.titleColor = titleColor;
    }

}
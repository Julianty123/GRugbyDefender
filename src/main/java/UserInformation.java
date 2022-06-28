import gearth.extensions.parsers.HPoint;
import java.util.LinkedList;

public class UserInformation {
    public int userIndex;
    public int userId;
    public String userName;
    public HPoint userPosition;

    public static final LinkedList<UserInformation> listUserInformation = new LinkedList<>();
    public static final LinkedList<Integer> flagListUserId = new LinkedList<>();


    public UserInformation(int userIndex, int userId, String userName, HPoint userPosition) {
        this.userIndex = userIndex;
        this.userId = userId;
        this.userName = userName;
        this.userPosition = userPosition;
    }

    public int getUserIndex() {
        return userIndex;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public HPoint getUserPosition() {
        return userPosition;
    }

    public void setUserPosition(HPoint userPosition) {
        this.userPosition = userPosition;
    }
}

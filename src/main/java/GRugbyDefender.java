import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.*;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyAdapter;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import javax.swing.Timer;
import java.util.*;

@ExtensionInfo(
        Title =  "GRugbyDefender",
        Description =  "Makes it easier to play the game",
        Version =  "1.0.2",
        Author =  "Julianty"
)

public class GRugbyDefender extends ExtensionForm {

    // Sirjonasxx Code Start here! //
    public CheckBox chkClickthrough;
    public Button enableBtn, buttonStart;
    public Label stateLbl;
    public CheckBox chkAlwaysOnTop;
    public AnchorPane statePane;
    // Sirjonasxx Code Finish here! //

    public CheckBox checkPlayersToFollow, checkIceCreams, checkLaserDoor, checkGrabCoord, chkRabbitEffect;
    public RadioButton radioButtonEast, radioButtonSouth, radioButFirst, radioButSecond;
    public Text textDefender, textBodyFacingD, textBodyFacingA, textDefenderX,
            textDefenderY, textAttacker, textAttackerX, textAttackerY;
    public TextField textFieldFirst, textFieldSecond;

    // Sirjonasxx Code Start here! //
    private volatile long latestPingTimestamp = -1;

    private volatile int ping = 45;
    private volatile double pingVariation = 10;
    private volatile long latestRoomTick = -1;
    private volatile long latestWalkTimestamp = -1;

    private volatile boolean rechargerEnabled = false;
    private volatile boolean clickthroughEnabled = false;
    // Sirjonasxx Code Finish here! //


    Map<Integer,HPoint> floorItemsID_HPoint = new HashMap<>();
    public LinkedList<Integer> listIceCreams = new LinkedList<>();    // Si se tienen miles de datos sera mas rapido
    public LinkedList<Integer> listPlayersToFollow = new LinkedList<>();
    public LinkedList<Integer> listLaserDoors = new LinkedList<>();

    public int YourID, Defender_X, Defender_Y, Attacker_X, Attacker_Y;
    public String YourName;
    public String stateLaser1 = "0";    // "0" is closed for defect
    public String stateLaser2 = "0";
    public int DefenderIndex = -1;
    public int AttackerIndex = -1;
    public int AttackerUserId;
    public String AttackerUserName;
    public int Iice = -1;
    public int AutoClickX, AutoClickY;

    Timer timerClick = new Timer(1, e -> {
        sendToServer(new HPacket("MoveAvatar" , HMessage.Direction.TOSERVER, AutoClickX, AutoClickY));
    });

    Timer timer1 = new Timer(1, e -> { // Se inunda el servidor cada 1ms (tener cuidado), aunque se podria usar 560ms pero es lento
        try {
            // Solo sigue al atacante que agarro el ultimo helado, pero previamente se deben almacenar todos los jugadores del equipo contrario
            for(UserInformation user: UserInformation.listUserInformation){
                if(user.getUserIndex() == AttackerIndex){
                    AttackerUserId = user.getUserId();
                }
            }

            if(listPlayersToFollow.contains(AttackerUserId)){
                if (radioButtonEast.isSelected()){
                    for (Map.Entry<Integer, HPoint> entry : floorItemsID_HPoint.entrySet()) {   // Evita quedarse estancado en pod (Vertical - Eje Y)
                        int Furni_Y = entry.getValue().getX();
                        if(Attacker_Y < Furni_Y && Defender_Y > Furni_Y){
                            sendToServer(new HPacket("MoveAvatar" , HMessage.Direction.TOSERVER, Defender_X, Furni_Y)); break;
                        }
                        else if(Attacker_Y > Furni_Y && Defender_Y < Furni_Y){
                            sendToServer(new HPacket("MoveAvatar" , HMessage.Direction.TOSERVER, Defender_X, Furni_Y)); break;
                        }
                    }
                    if (Attacker_Y < Defender_Y)
                    {
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER,Defender_X, Attacker_Y));
                    }
                    // if (NotYou_X == You_X) { Ext.SendToServerAsync(Ext.Out.MoveAvatar, Defender_X, Attacker_Y); }
                    else if(Attacker_Y > Defender_Y)
                    {
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, Defender_X, Attacker_Y));
                    }
                }

                else if (radioButtonSouth.isSelected())
                {
                    for (Map.Entry<Integer, HPoint> entry : floorItemsID_HPoint.entrySet()) {   // Evita quedarse estancado en pod (Horizontal - Eje X)
                        int Furni_X = entry.getValue().getX();
                        // Attacker_X + 1 == furni_X && Defender_X - 1 == furni_X
                        if(Attacker_X < Furni_X && Defender_X > Furni_X){ // Si la silla esta a la derecha del atacante y a la izquierda del defensor (yo)
                            sendToServer(new HPacket("MoveAvatar" , HMessage.Direction.TOSERVER, Furni_X, Defender_Y)); break;
                        }
                        // Attacker_X > furni_X && Defender_X + 1 == furni_X
                        else if(Attacker_X > Furni_X && Defender_X < Furni_X){ // Si la silla esta a la izquierda del atacante...
                            sendToServer(new HPacket("MoveAvatar" , HMessage.Direction.TOSERVER, Furni_X, Defender_Y)); break;
                        }
                    }
                    if (Attacker_X < Defender_X)
                    {
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER,Attacker_X, Defender_Y));
                    }
                    /* Esto es para corregir un bug ahi raro cuando no actualizaba las coordenadas, en el UserUpdate
                    if (NotYou_X == You_X) { Ext.SendToServerAsync(Ext.Out.MoveAvatar, Attacker_X, Defender_Y); } */
                    else if (Attacker_X > Defender_X)
                    {
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, Attacker_X, Defender_Y));
                    }
                }
            }
        }catch (IndexOutOfBoundsException ignored){ }
    });

    /* Podria servir para acceder a informacion desde un JSON
    https://www.jc-mouse.net/javafx/javafx-y-apirest-get
     */


    /* Ignore this
     @Before (que ejecuta alguna declaración / condición previa antes de @Test, public void),
     @After (que ejecuta alguna declaración después de @Test, public void, por ejemplo, restablecer variables,
     eliminar archivos temporales, variables , etc.)
     @Ignore (que ignora alguna declaración durante la ejecución de la prueba; tenga en cuenta que
     @BeforeClass y @AfterClass se utilizan para ejecutar declaraciones antes y después de todos los casos de prueba,
     public static void, respectivamente).
     */

    @Override
    protected void onShow() {
        // When the extension is installed!
        sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));//Get your name, your id, etc
        sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));//Get flooritems, wallitems and other stuffs
        sendToServer(new HPacket("AvatarExpression", HMessage.Direction.TOSERVER, 0));// Get your index
    }

    @Override
    protected void initExtension() {
        primaryStage.setOnShowing(s -> { // Cuando se abre la extension se inicia el Hook
            // More information     https://github.com/kristian/system-hook
            // Might throw a UnsatisfiedLinkError if the native library fails to load or a RuntimeException if hooking fails
            GlobalKeyboardHook keyboardHook = new GlobalKeyboardHook(false); // Use false here to switch to hook instead of raw input
            keyboardHook.addKeyListener(new GlobalKeyAdapter() {
                @Override // Se ejecuta el tiempo que se mantenga presionada la tecla
                public void keyPressed(GlobalKeyEvent event) {
                    // System.out.println("keychar: " + event.getKeyChar()); other way maybe
                    if(event.getVirtualKeyCode() == 112){ // Key F1
                        if(!"".equals(textFieldFirst.getText())){
                            // I suppose this it will do more fast
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldFirst.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldFirst.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldFirst.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldFirst.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldFirst.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldFirst.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldFirst.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldFirst.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldFirst.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldFirst.getText())));
                        }
                    }
                    if(event.getVirtualKeyCode() == 113){ // Key F2   Key 2 is 50 VirtualKeyCode
                        if(!"".equals(textFieldSecond.getText())){
                            // I suppose this it will do more fast
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldSecond.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldSecond.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldSecond.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldSecond.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldSecond.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldSecond.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldSecond.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldSecond.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldSecond.getText())));
                            sendToServer(new HPacket("PassCarryItem", HMessage.Direction.TOSERVER, Integer.parseInt(textFieldSecond.getText())));
                        }
                    }
                    if(event.getVirtualKeyCode() == 117){ // Key F6
                        StartDefenderBot();
                    }
                    if(event.getVirtualKeyCode() == 118){ // Key F7
                        enableButtonClick();
                    }
                    if(event.getVirtualKeyCode() == 119){ // Key F8
                        if(checkGrabCoord.isSelected()){
                            checkGrabCoord.setSelected(false);
                        }
                        else {checkGrabCoord.setSelected(true); checkGrabSelected();}
                    }
                }
            });
            primaryStage.setOnCloseRequest(e -> { // Cuando se cierra la ventana se finaliza el Hook
                keyboardHook.shutdownHook(); // Disabled hook

                stateLaser1 = "0";  stateLaser2 = "0";  // "0" is closed for defect
                UserInformation.listUserInformation.clear();    UserInformation.flagListUserId.clear();
                disable();  chkClickthrough.setSelected(false); ClearAll();
                sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT,chkClickthrough.isSelected()));
            });
        });

        // Response of packet AvatarExpression
        intercept(HMessage.Direction.TOCLIENT, "Expression", hMessage -> {
            // First integer is index in room, second is animation id, i think
            if(primaryStage.isShowing() && DefenderIndex == -1){ // this could avoid any bug
                DefenderIndex = hMessage.getPacket().readInteger();   // Se le asigna TU index actual
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            YourID = hMessage.getPacket().readInteger();    YourName = hMessage.getPacket().readString();
            textDefender.setText("Defender : " + YourName);
        });

        // Expression y UserObject se ejecutan en diferente orden entonces se intercepta este paquete para evitar bugs
        intercept(HMessage.Direction.TOCLIENT, "GetGuestRoomResult", hMessage -> {
            /*if(!Id_Name_Index.contains(String.valueOf(YourID))){
                Id_Name_Index.add(String.valueOf(YourID));
                Id_Name_Index.add(YourName);
                Id_Name_Index.add(String.valueOf(DefenderIndex));
            }*/
        });

        // Intercepts when the user gives you the drink
        intercept(HMessage.Direction.TOCLIENT, "HandItemReceived", hMessage -> {
            int userIndex = hMessage.getPacket().readInteger();
            int drinkId = hMessage.getPacket().readInteger();
        });

        // Intercept FloorItems when you restart the room
        intercept(HMessage.Direction.TOCLIENT, "Objects", hMessage -> {
            floorItemsID_HPoint.clear(); // Elimina la lista al entrar a una sala
            HPacket hPacket = hMessage.getPacket();
            for (HFloorItem floorItem: HFloorItem.parse(hPacket)){
                floorItemsID_HPoint.put(floorItem.getId(), floorItem.getTile()); // Agrega la Key y Value al Diccionario
                /*if(floorItem.getTile().getX() == 7 && floorItem.getTile().getY() == 10){
                    System.out.println("Tile: " + floorItem.getTile() + " ID blanco: " + floorItem.getId() + " TypeID: "
                            + floorItem.getTypeId() + " Stuff: " + Arrays.toString(floorItem.getStuff()));
                }*/
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            try {
                HPacket hPacket = hMessage.getPacket();
                HEntity[] roomUsersList = HEntity.parse(hPacket);
                for (HEntity hEntity: roomUsersList){
                    UserInformation userInf = new UserInformation(hEntity.getIndex(), hEntity.getId(), hEntity.getName(), hEntity.getTile());
                    if(hEntity.getEntityType().equals(HEntityType.HABBO)){ // Verifica que NO sea bot
                        if(hEntity.getName().equals(YourName)){    // In another room, the index changes
                            DefenderIndex = hEntity.getIndex();      // The userindex has been restarted
                        }
                        if(!UserInformation.flagListUserId.contains(hEntity.getId())) {
                            UserInformation.listUserInformation.add(userInf);
                            UserInformation.flagListUserId.add(hEntity.getId()); // Bandera para saber si el objeto ya existe

                            // Antes creaba varias listas para agregar diferentes valores y encontrar sus correspondientes parejas
                        }
                        else {
                            int positionUserId = UserInformation.flagListUserId.indexOf(hEntity.getId()); // Obtiene la posicion de ese userId
                            UserInformation.listUserInformation.set(positionUserId, userInf); // Actualiza los valores!

                            // Antes con la posicion del objeto en una lista, averiguaba la posiciones de los demas objetos en las otras listas
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Se intercepta cuando se reinicia sala o se entra a una
        intercept(HMessage.Direction.TOCLIENT, "YouArePlayingGame", hMessage -> {
            sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT,chkClickthrough.isSelected()));
        });

        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            HPacket hPacket = hMessage.getPacket();
            for (HEntityUpdate hEntityUpdate: HEntityUpdate.parse(hPacket)){
                for(UserInformation user: UserInformation.listUserInformation){
                    if(user.getUserIndex() == hEntityUpdate.getIndex() && hEntityUpdate.getMovingTo() != null){
                        user.setUserPosition(hEntityUpdate.getMovingTo()); // Actualiza la posicion de los usuarios seleccionados
                        break;
                    }
                }
                try{
                    if (hEntityUpdate.getIndex() == AttackerIndex) {
                        Attacker_X = hEntityUpdate.getMovingTo().getX();    Attacker_Y = hEntityUpdate.getMovingTo().getY();
                        textAttackerX.setText("x: " + Attacker_X);  textAttackerY.setText("y: " + Attacker_Y);
                        textBodyFacingA.setText(String.valueOf(hEntityUpdate.getBodyFacing()));
                    }
                    if (hEntityUpdate.getIndex() == DefenderIndex) {
                        Defender_X = hEntityUpdate.getMovingTo().getX();    Defender_Y = hEntityUpdate.getMovingTo().getY();
                        textDefenderX.setText("x: " + Defender_X);   textDefenderY.setText("y: " + Defender_Y);
                        textBodyFacingD.setText(String.valueOf(hEntityUpdate.getBodyFacing()));

                        if(((AutoClickX == Defender_X) && AutoClickY == Defender_Y)){
                            timerClick.stop();
                            AutoClickX = 0; AutoClickY = 0;
                            Platform.runLater(()-> checkGrabCoord.setText("Grab coord [F8]: (" + AutoClickX + ", " + AutoClickY + ")"));
                            sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "The AutoClick has been stopped!", 0, 21, 0, -1));
                        }

                        // Example id (340594432): first ice (down)
                        if( floorItemsID_HPoint.get(listIceCreams.get(0)).getX()-3 <= Defender_X && floorItemsID_HPoint.get(listIceCreams.get(0)).getY()-5<= Defender_Y){
                            Iice = 1; // Here, you are in the up zone
                            System.out.println(Iice);
                        }

                        // Example id (137740379): second ice (up)
                        if( floorItemsID_HPoint.get(listIceCreams.get(1)).getX()-3 <= Defender_X && floorItemsID_HPoint.get(listIceCreams.get(1)).getY()+5 >= Defender_Y){
                            Iice = 0; // Here, you are in the down zone
                            System.out.println(Iice);
                        }
                    }
                }catch(NullPointerException | NumberFormatException | IndexOutOfBoundsException ignored){ }
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "CarryObject", hMessage -> {
            try {
                int currentUserIndex = hMessage.getPacket().readInteger(); // Index del usuario que recibio la bebida
                int HandItemID = hMessage.getPacket().readInteger();

                for(UserInformation user: UserInformation.listUserInformation){
                    if(user.getUserIndex() == currentUserIndex){
                        AttackerUserId = user.getUserId(); // Asigna el userId
                        AttackerUserName = user.getUserName();  // Asigna el nombre de usuario actual!
                        break;
                    }
                }

                if(listPlayersToFollow.contains(AttackerUserId)){
                    if (HandItemID == 0){   // 0 cuando el usuario pasa o suelta la bebida
                        sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, currentUserIndex, 0, 0)); // Le quita el efecto al usuario
                    }
                    else {
                        AttackerIndex = currentUserIndex;   // Actualiza el userIndex
                        textAttacker.setText("Current Attacker : " + AttackerUserName);

                        if(chkRabbitEffect.isSelected()){   // Added the user effect if have drink (ClientSide), 68 is the Rabbit Effect
                            sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, AttackerIndex, 68, 0));
                        }

                        for(UserInformation user: UserInformation.listUserInformation){
                            if(user.getUserIndex() == AttackerIndex){
                                Attacker_X = user.getUserPosition().getX(); Attacker_Y = user.getUserPosition().getY();
                                textAttackerX.setText("x: " + Attacker_X);  textAttackerY.setText("y: " + Attacker_Y);
                                break;
                            }
                        }
                    }
                }
            }catch (IndexOutOfBoundsException ignored){}
        });

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", hMessage -> {
            if(checkGrabCoord.isSelected()){
                AutoClickX = hMessage.getPacket().readInteger();
                AutoClickY = hMessage.getPacket().readInteger();
                Platform.runLater(()-> checkGrabCoord.setText("Grab coord: (" + AutoClickX + ", " +  AutoClickY + ")"));
                sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "The coord has been record and the " +
                        "AutoClick has started", 0, 21, 0, -1));
                checkGrabCoord.setSelected(false);
                hMessage.setBlocked(true);
                timerClick.start();
            }
        });

        intercept(HMessage.Direction.TOSERVER, "GetSelectedBadges", hMessage -> {
            int UserID = hMessage.getPacket().readInteger();
            if (checkPlayersToFollow.isSelected())
            {
                if (!listPlayersToFollow.contains(UserID))
                {
                    listPlayersToFollow.add(UserID);
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "The id: " + UserID + ", has been added.", 0, 21, 0, -1));
                }else{
                    listPlayersToFollow.remove((Integer) UserID);
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "The id: " + UserID + " has been deleted!", 0, 21, 0, -1));
                }
                Platform.runLater(() -> checkPlayersToFollow.setText("Players to Follow (" + listPlayersToFollow.size() + ")"));
                checkPlayersToFollow.setSelected(false);
            }
            if(radioButFirst.isSelected()){
                Platform.runLater(()->{
                    textFieldFirst.setText(String.valueOf(UserID)); radioButFirst.setSelected(false);
                });
            }
            else if(radioButSecond.isSelected()){
                Platform.runLater(()->{
                    textFieldSecond.setText(String.valueOf(UserID)); radioButSecond.setSelected(false);
                });
            }
        });

        intercept(HMessage.Direction.TOSERVER, "UseFurniture", hMessage -> {
            if (checkIceCreams.isSelected())
            {
                int IceCreamID = hMessage.getPacket().readInteger();
                if (!listIceCreams.contains(IceCreamID))
                {
                    listIceCreams.add(IceCreamID);
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "ice cream with id: " + IceCreamID + " added!", 0, 21, 0, -1));
                }
                else
                {
                    listIceCreams.remove((Integer) IceCreamID); // Es rara esta linea de codigo pero funciona
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "ice cream with id: " + IceCreamID + " deleted!", 0, 21, 0, -1));
                }
                checkIceCreams.setSelected(false);
                Platform.runLater(()-> checkIceCreams.setText("Select ice creams (" + listIceCreams.size() + ")"));
            }
            if(checkLaserDoor.isSelected()){
                int laserDoorID = hMessage.getPacket().readInteger();
                if(!listLaserDoors.contains(laserDoorID)){
                    listLaserDoors.add(laserDoorID);
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "Laser door: " + laserDoorID + " has been added!", 0, 21, 0, -1));

                }
                else {
                    listLaserDoors.remove((Integer) laserDoorID);
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "Laser door: " + laserDoorID + " has been deleted!", 0, 21, 0, -1));
                }
                Platform.runLater(()-> checkLaserDoor.setText("Select laser door (" + listLaserDoors.size() + ")"));
                hMessage.setBlocked(true);
            }
        });

        // When a furni changes your state for this case in the laser door
        intercept(HMessage.Direction.TOCLIENT, "ObjectDataUpdate", hMessage -> {
            int DoorLaserID = Integer.parseInt(hMessage.getPacket().readString());
            Object nothing = hMessage.getPacket().readInteger();
            String stateLaserDoor = hMessage.getPacket().readString();  // "1" = Opened, "0" = Closed
            try {
                if(listLaserDoors.get(0) == DoorLaserID){
                    stateLaser1 = stateLaserDoor;
                }
                if(listLaserDoors.get(1) == DoorLaserID){
                    stateLaser2 = stateLaserDoor;
                }
            }catch (IndexOutOfBoundsException ignored){}
            if(stateLaser1.equals("1") && stateLaser2.equals("1")){
                disable(); // Click recharger stopped
            }

            // laserDoorId_State.keySet().iterator().next() // Obtiene la primera clave del map
            //laserDoorId_State.keySet().stream().findFirst().get(); // Otra forma de obtener la primera clave
            // Permite recorrer todas las claves y valores de un Map
                /*for (Map.Entry<Integer, String> entry : laserDoorId_State.entrySet()) {
                    System.out.println("key: " + entry.getKey() + ", value: " + entry.getValue());
                }*/
        });

        // Sirjonasxx Code Start Here //
        intercept(HMessage.Direction.TOSERVER, "LatencyPingRequest", hMessage -> {
            latestPingTimestamp = System.currentTimeMillis();
        });

        intercept(HMessage.Direction.TOCLIENT, "LatencyPingResponse", hMessage -> {
            if (latestPingTimestamp != -1) {
                int newPing = (int) (System.currentTimeMillis() - latestPingTimestamp) / 2;
                pingVariation = pingVariation * 0.66 + (Math.abs(ping - newPing)) * 0.34;
                if (pingVariation > 10) {
                    pingVariation = 10;
                }
                ping = newPing;
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            if (Arrays.stream(HEntityUpdate.parse(hMessage.getPacket()))
                    .anyMatch(hEntityUpdate -> hEntityUpdate.getAction() == HAction.Move) &&
                    System.currentTimeMillis() > latestRoomTick + 400) {
                latestRoomTick = System.currentTimeMillis() - ping;
            }
        });

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", this::onUserWalk);

        intercept(HMessage.Direction.TOCLIENT, "RoomReady", hMessage -> reset());

        new Thread(() -> {
            while(true) {
                Functions.sleep(1550);
                if (rechargerEnabled) {
                    try {
                        sendToServer(new HPacket("UseFurniture", HMessage.Direction.TOSERVER, listIceCreams.get(Iice), 0));
                    }catch (IndexOutOfBoundsException ignored){}
                }
            }
        }).start();
        // Sirjonasxx Code Finish here! //
    }

    // Sirjonasxx Code Start here! //
    public void initialize() {
        reset();
    }

    private volatile boolean isClicking = false;

    private void onUserWalk(HMessage hMessage) {
        if (!clickthroughEnabled) {
            return;
        }

        if (isClicking) {
            hMessage.setBlocked(true);
            return;
        }

        long now = System.currentTimeMillis();
        if (now >= latestWalkTimestamp + 512) {
            latestWalkTimestamp = System.currentTimeMillis();

        }
        // if click is expected to be able to happen in current room tick
        else if (latestWalkTimestamp + 512 < -getTimeSinceTick() + 500 + now - ping - pingVariation - 10) {
            int awaitTime = (int) (latestWalkTimestamp + 512 - now);
            hMessage.setBlocked(true);
            isClicking = true;
            new Thread(() -> {
                Functions.sleep(awaitTime);
                sendToServer(hMessage.getPacket());
                latestWalkTimestamp = System.currentTimeMillis();
                isClicking = false;
            }).start();
        }
    }
    private int getTimeSinceTick() {
        if (latestRoomTick == -1) {
            return 0;
        }

        long now = System.currentTimeMillis();
        return (int) ((now - latestRoomTick) + 500) % 500;
    }

    private void resetGClick() {
        rechargerEnabled = false;

        Platform.runLater(() -> {
            stateLbl.setText("Inactive");
            enableBtn.setText("Enable [Key F7]");
            statePane.setBackground(new Background(new BackgroundFill(Paint.valueOf("#F9B2B0"), CornerRadii.EMPTY, Insets.EMPTY)));
        });
    }

    private void reset() {
        resetGClick();

        clickthroughEnabled = false;
        Platform.runLater(() -> chkClickthrough.setSelected(false));
    }

    public void clickthroughClick(ActionEvent actionEvent) {
        clickthroughEnabled = chkClickthrough.isSelected();
        sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, clickthroughEnabled));
    }

    public void alwaysOnTopClick(ActionEvent actionEvent) {
        primaryStage.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
    }

    private void disable() {
        resetGClick();
    }

    public void enableClick(ActionEvent actionEvent) {
        enableButtonClick();
    }

    public void enableButtonClick(){
        if (enableBtn.getText().equals("Enable [Key F7]")) {
            if( listIceCreams.size() < 2){
                sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "Select both ice creams, please", 0, 21, 0, -1));
            }
            else if(Iice == -1){
                sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "You need to walk over an area (friend or enemy) to use G-Click", 0, 21, 0, -1));
            }
            else if(listIceCreams.size() == 2){
                rechargerEnabled = true;
                Platform.runLater(() -> {
                    stateLbl.setText("Enabled");
                    enableBtn.setText("Abort [Key F7]");
                    statePane.setBackground(new Background(new BackgroundFill(Paint.valueOf("#9AFD9F"), CornerRadii.EMPTY, Insets.EMPTY)));
                });
            }
        }
        else {
            disable();
        }
    }
    // Sirjonasxx Code Finish here! //

    public void handleStart(ActionEvent actionEvent) {
        StartDefenderBot();
    }

    public void StartDefenderBot(){
        if("OFF [Key F6]".equals(buttonStart.getText())){
            Platform.runLater(() ->{
                buttonStart.setText("ON [Key F6]");
                timer1.start();
            });
        }
        else {
            Platform.runLater(() ->{
                buttonStart.setText("OFF [Key F6]");
                timer1.stop();
            });
        }
    }

    public void handleCheckGrab(ActionEvent actionEvent) {
        checkGrabSelected();
    }

    public void checkGrabSelected(){
        if(checkGrabCoord.isSelected()){
            AutoClickX = 0; AutoClickY = 0;
            Platform.runLater(()->{
                checkGrabCoord.setText("Grab coord [F8]: (" + AutoClickX + ", "  + AutoClickY + ")");
            });
            if(timerClick.isRunning()){
                timerClick.stop();
                sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "The AutoClick has been stopped!", 0, 21, 0, -1));
            }
        }
    }


    public void ClearAll(){
        // -- Players
        listPlayersToFollow.clear();
        Platform.runLater(() ->{
            checkPlayersToFollow.setText("Players to Follow (" + listPlayersToFollow.size() + ")");
        });
        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "Players to follow has been deleted successfully!", 0, 21, 0, -1));

        // -- Ice creams
        listIceCreams.clear();
        Platform.runLater(() ->{
            checkIceCreams.setText("Select ice creams (" + listPlayersToFollow.size() + ")");
        });
        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "Ice creams has been deleted successfully!", 0, 21, 0, -1));

        // Laser doors
        listLaserDoors.clear();
        Platform.runLater(() ->{
            checkLaserDoor.setText("Select laser door (" + listPlayersToFollow.size() + ")");
        });
        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 99999, "Laser doors has been deleted successfully!", 0, 21, 0, -1));
        stateLaser1 = "0";  stateLaser2 = "0";  // "0" is closed for defect

        //Defender_X = -1;
        //Defender_Y = -1;
        Attacker_X = -1;
        Attacker_Y = -1;
        // DefenderIndex = -1;
        AttackerIndex = -1;
        Iice = -1;
        textAttackerX.setText("x: 0");  textAttackerY.setText("y: 0");
        textAttacker.setText("Current Attacker: NONE");
    }

    public void handleClearAll(ActionEvent actionEvent) {
        ClearAll();
    }

    public void handleRabbitEffect(ActionEvent actionEvent) {
        if(chkRabbitEffect.isSelected()){
            sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, AttackerIndex, 68, 0)); // Add effect
        }else {
            sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, AttackerIndex, 0, 0)); // Remove effect
        }
    }
}


/*
ChatConsole chat = new ChatConsole(this);

        intercept(HMessage.Direction.TOSERVER, "Chat", hMessage -> {
            chat.writeOutput(hMessage.getPacket().readString(), false);
        });
 */
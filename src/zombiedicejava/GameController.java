package zombiedicejava;

import java.beans.PropertyChangeSupport;
import java.util.List;
import javax.swing.SwingWorker;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author ENG Eldeeb
 */
public class GameController extends SwingWorker<Void, Object> {

    ZombieDice model = null;
    Game view = null;
    GameController(Game view, ZombieDice model) {
        this.model = model;
        this.view = view;
    }

    @Override
    public Void doInBackground() {
        model.startGame();
        return null;
    }

    public void callPublish(Object data) {
        this.publish(data);
    }
    int choice=0;
    public void process(List<Object> data) {
        for (Object item : data) {
            if (item instanceof Object[][]) {
                view.updateLeaderboard((Object[][]) item);
            } else if (item instanceof String) {
                String str = (String) item;
                view.updateActionLog(str);
                if(str.endsWith("*"))view.endGame();
            }
            else if(item instanceof ZombieDice.Player){
                view.updateChoice((ZombieDice.Player)item);
            }
        }
    }

}

package zombiedicejava;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

enum DiceColor {
    RED, YELLOW, GREEN;
}

enum FaceType {
    SHOTGUN, FEET, BRAINS;
}

class ZombieDice {

    PropertyChangeSupport pcs;
    void sortLeaderboard() {
        Collections.sort(leaderboard, sortByScore);
        Object[][] data = new Object[party.size()][3];
        for (int i = 0; i < data.length; i++) {
            data[i][0] = (Object) leaderboard.get(i).name;
            data[i][1] = (Object) leaderboard.get(i).brainScore;
            data[i][2] = (Object) leaderboard.get(i).deaths;
        }
        this.controller.callPublish(data);
    }
    GameController controller = null;
    final static int N = 13;
    int shotgunLimit = 3;
    static int onHandLimit = 3;
    Random rand = new Random(System.currentTimeMillis());

    class Player {

        String name;
        int brainScore;
        int shotgunCount;
        int feetDice;
        int deaths;
        int turns;
        boolean endTurn;
        ArrayList<Dice> onHand;

        Player(String name) {
            this.name = name;
            this.brainScore = this.shotgunCount = this.feetDice = this.deaths = this.turns = 0;
            this.endTurn = false;
            this.onHand = new ArrayList<Dice>();
        }

        @Override
        public String toString() {
            return name + " (" + brainScore + " brains, " + shotgunCount + " shotguns, " + feetDice + " feet)";
        }

        boolean isShot() {
            if (shotgunCount >= shotgunLimit) {
                brainScore = 0;
                shotgunCount = 0;
                feetDice = 0;
                deaths += 1;
                turns = 0;
                this.onHand.clear();
                return true;
            }
            return false;
        }

        void reroll() {
            Collections.shuffle(this.onHand);
            for (int i = 0; i < this.onHand.size(); i++) {
                Dice currentDice = this.onHand.get(i);
                int r = rand.nextInt(6);
                FaceType currentFace = currentDice.faces.get(r);
                // gameGUI.updateAnimation("reroll");
                if (currentFace == FaceType.BRAINS) {
                    brainScore += 1;
                    this.onHand.get(i).toRemove = true;
                    controller.callPublish(name + " rolled a " + currentDice.color + " dice and got a BRAIN!");
                } else if (currentFace == FaceType.SHOTGUN) {
                    shotgunCount += 1;
                    this.onHand.get(i).toRemove = true;
                    controller.callPublish(name + " rolled a " + currentDice.color + " dice and got a SHOTGUN!");
                } else {
                    feetDice += 1;
                    controller.callPublish(name + " rolled a " + currentDice.color + " dice and got a FOOT!");
                }
            }
            this.onHand.removeIf(n -> n.toRemove == true);
        }
    }

    class Dice {

        DiceColor color;
        ArrayList<FaceType> faces;
        boolean toRemove = false;

        Dice(DiceColor color) {
            this.color = color;
            faces = new ArrayList<FaceType>();
            faces.add(FaceType.FEET);
            faces.add(FaceType.FEET);
            faces.add(FaceType.SHOTGUN);
            faces.add(FaceType.BRAINS);
            if (color == DiceColor.RED) {
                faces.add(FaceType.SHOTGUN);
                faces.add(FaceType.SHOTGUN);
            } else if (color == DiceColor.YELLOW) {
                faces.add(FaceType.SHOTGUN);
                faces.add(FaceType.BRAINS);
            } else {
                faces.add(FaceType.BRAINS);
                faces.add(FaceType.BRAINS);
            }
        }
    }

    class Cup {

        ArrayList<Dice> dices;

        Cup() {
            refill();
        }

        boolean isEmpty() {
            return dices.size() <= 0;
        }

        void refill() {

            dices = new ArrayList<Dice>();
            for (int i = 0; i < N; i++) {
                int r = rand.nextInt(3);
                dices.add((r == 0) ? new Dice(DiceColor.RED)
                        : (r == 1) ? new Dice(DiceColor.YELLOW) : new Dice(DiceColor.GREEN));
            }
        }

        void shakeAndTake() {
            Collections.shuffle(dices);
            //   gameGUI.updateAnimation("shake");
            while (current.onHand.size() < onHandLimit) {
                int r = rand.nextInt(dices.size());
                current.onHand.add(dices.remove(r));
                if (isEmpty()) {
                    refill();
                }
            }
        }
    }

    ArrayList<Player> party;
    ArrayList<Player> leaderboard;
    Cup cup;
    public Comparator<Player> sortByScore = new Comparator<Player>() {
        @Override
        public int compare(Player p1, Player p2) {
            return p2.brainScore - p1.brainScore;
        }
    };

    ZombieDice(ArrayList<String> players) {
        pcs = new PropertyChangeSupport(this);
        pcs.addPropertyChangeListener((e) -> {
            if (e.getPropertyName().equals("choice")) {
                choice = (Integer) e.getNewValue();
                if (choice == 1) {
                    rollDice();
                } else if (choice == 2) {
                    passTurn();
                }
            }
        });
        this.party = new ArrayList<Player>();
        this.leaderboard = new ArrayList<Player>();
        this.cup = new Cup();
        for (int i = 0; i < players.size(); i++) {
            this.party.add(new Player(players.get(i)));
            this.leaderboard.add(this.party.get(i));
        }
        Collections.shuffle(this.party);
    }

    Player suddenDeath(ArrayList<Player> winners) {
        controller.callPublish("Entering SUDDEN DEATH");
        onHandLimit = 13;
        shotgunLimit = 9999;
        ArrayList<Player> suddenDeathPlayers = new ArrayList<Player>(winners);
        do {
            rounds++;
            for (Player winner : suddenDeathPlayers) {
                winner.onHand.clear();
                current = winner;
                rollDice();
            }
            Collections.sort(leaderboard, sortByScore);
            suddenDeathPlayers.removeIf(n -> n.brainScore < leaderboard.get(0).brainScore);
        } while (suddenDeathPlayers.size() > 1);
        return suddenDeathPlayers.get(0);
    }

    public void delay(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // Handle the interruption
            e.printStackTrace();
        }
    }

    Player checkWinner() {
        ArrayList<Player> winners = new ArrayList<Player>();
        for (Player player : this.leaderboard) {
            if (player.brainScore >= N) {
                winners.add(player);
            }
        }
        if (winners.size() == 0) {
            return null;
        } else if (winners.size() == 1) {
            return winners.get(0);
        } else {
            return suddenDeath(winners);
        }
    }

    void rollDice() {
        current.endTurn = false;
        //notify gui that player is rolling
        this.controller.callPublish(current.name + " has decided to keep rolling!");
        current.turns++;
        if (ghostTurn.turns < current.turns) {
            ghostTurn = current;
        }
        this.cup.shakeAndTake();
        current.reroll();
        if (current.isShot()) {
            //notify gui that player is shot
            this.controller.callPublish(current.name + " has been shot and lost all their brains!");
            passTurn();
        }
    }

    void passTurn() {
        //notify gui that player is passing turn
        this.controller.callPublish(current.name + " has decided to pass their turn!");
        current.turns = 0;
        current.shotgunCount = 0;
        current.feetDice = 0;
        current.endTurn = true;
        this.cup.dices.addAll(0, current.onHand);
    }
    int choice = 0;

    void rollOrPass() {
        this.controller.callPublish(current);
        while (choice == 0) {
            delay(100);
        }
    }
    Player ghostDeath, ghostBrain, ghostTurn, current;
    int rounds = 0;
    Player winner = null;
    void startGame() {
        ghostDeath = ghostBrain = ghostTurn = current = new Player("Nobody");
        sortLeaderboard();
        do {
            rounds++;
            for (int i = 0; i < this.party.size(); i++) {
                current = this.party.get(i);
                this.controller.callPublish(current.name + "'s Turn");
                while (!current.endTurn) {
                    rollOrPass();
                    sortLeaderboard();
                    delay(100);
                }
                current.endTurn = false;
                if (ghostBrain.brainScore < current.brainScore) {
                    ghostBrain = current;
                }
                if (ghostDeath.deaths < current.deaths) {
                    ghostDeath = current;
                }
            }
            winner = checkWinner();
        } while (winner == null);
        this.controller.callPublish(winner.name + " has collected " + winner.brainScore + " brains and won the game!*");
        return;
    }
}

package HandGame;

class GameLogic {
    private String player1;
    private String player2;

    public GameLogic(String player1, String player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public String determineWinner(int player1Gauge, int player2Gauge) {
        if (player1Gauge > player2Gauge) {
            return player1;
        } else if (player1Gauge < player2Gauge) {
            return player2;
        } else {
            return "비겼습니다!";
        }
    }
}
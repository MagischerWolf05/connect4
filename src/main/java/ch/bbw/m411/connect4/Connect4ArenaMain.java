package ch.bbw.m411.connect4;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Plays a game of Connect Four on a 4x7 board (a variation of the original 6x7 board).
 * The pieces fall straight down, occupying the lowest available space within the column.
 */
public class Connect4ArenaMain {

	static final int WIDTH = 7;

	static final int HEIGHT = 4;

	static final int NOMOVE = -1;

	public static void main(String[] args) {
	new Connect4ArenaMain().play(new HumanPlayer(), new AlphaBetaPlayer());
	}

	static String toDebugString(Stone[] board) {
		var sb = new StringBuilder();
		for (int r = 0; r < HEIGHT; r++) {
			for (int c = 0; c < WIDTH; c++) {
				var value = board[r * WIDTH + c];
				sb.append(value == null ? "." : (value == Stone.RED ? "X" : "O"));
			}
			sb.append("-");
		}
		return sb.toString();
	}


	Connect4Player play(Connect4Player red, Connect4Player blue) {
		if (red == blue) {
			throw new IllegalStateException("must be different players (simply create two instances)");
		}
		var board = new Stone[WIDTH * HEIGHT];
		red.initialize(Arrays.copyOf(board, board.length), Stone.RED);
		blue.initialize(Arrays.copyOf(board, board.length), Stone.BLUE);
		var lastMove = NOMOVE;
		var currentPlayer = red;
		for (int round = 0; round < board.length; round++) {
			var currentColor = currentPlayer == red ? Stone.RED : Stone.BLUE;
			System.out.println(HumanPlayer.toPrettyString(board) + currentColor + " to play next...");
			lastMove = currentPlayer.play(lastMove);
			if (lastMove < 0 || lastMove >= WIDTH * HEIGHT ||
					board[lastMove] != null && (lastMove < WIDTH || board[lastMove - WIDTH] != null)) {
				throw new IllegalStateException("cannot play to position " + lastMove + " @ " + toDebugString(board));
			}
			board[lastMove] = currentColor;
			if (isWinning(board, currentColor)) {
				System.out.println(
						HumanPlayer.toPrettyString(board) + "...and the winner is: " + currentColor + " @ " + toDebugString(board));
				return currentPlayer;
			}
			currentPlayer = currentPlayer == red ? blue : red;
		}
		System.out.println(HumanPlayer.toPrettyString(board) + "...it's a DRAW @ " + toDebugString(board));
		return null; // null implies a draw
	}

	public static boolean isWinning(Stone[] board, Stone color) {
		// check for horizontal wins
		for (int row = 0; row < HEIGHT; row++) {
			for (int col = 0; col < WIDTH-3; col++) {
				if (board[row*WIDTH+col] == color &&
						board[row*WIDTH+col+1] == color &&
						board[row*WIDTH+col+2] == color &&
						board[row*WIDTH+col+3] == color) {
					return true;
				}
			}
		}

		// check for vertical wins
		for (int row = 0; row < HEIGHT-3; row++) {
			for (int col = 0; col < WIDTH; col++) {
				if (board[row*WIDTH+col] == color &&
						board[(row+1)*WIDTH+col] == color &&
						board[(row+2)*WIDTH+col] == color &&
						board[(row+3)*WIDTH+col] == color) {
					return true;
				}
			}
		}

		// check for diagonal wins
		for (int row = 0; row < HEIGHT-3; row++) {
			for (int col = 0; col < WIDTH-3; col++) {
				if (board[row*WIDTH+col] == color &&
						board[(row+1)*WIDTH+col+1] == color &&
						board[(row+2)*WIDTH+col+2] == color &&
						board[(row+3)*WIDTH+col+3] == color) {
					return true;
				}
			}
		}

		for (int row = 3; row < HEIGHT; row++) {
			for (int col = 0; col < WIDTH-3; col++) {
				if (board[row*WIDTH+col] == color &&
						board[(row-1)*WIDTH+col+1] == color &&
						board[(row-2)*WIDTH+col+2] == color &&
						board[(row-3)*WIDTH+col+3] == color) {
					return true;
				}
			}
		}
		return false;
	}




	public enum Stone {
		RED, BLUE;

		public Stone opponent() {

			return this == RED ? BLUE : RED;
		}
	}

	public interface Connect4Player {

		/**
		 * Called before the game starts and guaranteed to only be called once per livetime of the player.
		 *
		 * @param board the starting board, usually an empty board.
		 * @param colorToPlay the color of this player
		 */
		void initialize(Stone[] board, Stone colorToPlay);

		/**
		 * Perform a next move, will only be called if the Game is not over yet.
		 * Each player has to keep an internal state of the 4x7 board, wher the 0-index is on the bottom row.
		 * The index-layout looks as:
		 * <pre>
		 * 30 31 32 33 34 35 36
		 * 14 15 16 17 18 19 29
		 *  7  8  9 10 11 12 13
		 *  0  1  2  3  4  5  6
		 * </pre>
		 *
		 * @param opponendPlayed the last index where the opponent played to (in range 0 - width*height exclusive)
		 * or -1 if this is the first move.
		 * @return an index to play to (in range 0 - width*height exclusive)
		 */
		int play(int opponendPlayed);
	}

	/**
	 * An abstract helper class to keep track of a board (and whatever we or the opponent played).
	 */
	public abstract static class DefaultPlayer implements Connect4Player {

		Stone[] board;

		Stone myColor;

		@Override
		public void initialize(Stone[] board, Stone colorToPlay) {
			this.board = board;
			myColor = colorToPlay;
		}

		@Override
		public int play(int opponendPlayed) {
			if (opponendPlayed != NOMOVE) {
				board[opponendPlayed] = myColor.opponent();
			}
			var playTo = play();
			board[playTo] = myColor;
			return playTo;
		}

		/**
		 * Givent the current {@link #board}, find a suitable position-index to play to.
		 * @return the position to play to as defined by {@link Connect4Player#play(int)}.
		 */
		abstract int play();

	}
	public static class HumanPlayer extends DefaultPlayer {

		static String toPrettyString(Stone[] board) {
			var sb = new StringBuilder();
			for (int r = HEIGHT - 1; r >= 0; r--) {
				for (int c = 0; c < WIDTH; c++) {
					var index = r * WIDTH + c;
					if (board[index] == null) {
						if (index < WIDTH || board[index - WIDTH] != null) {
							sb.append("\033[37m" + index + "\033[0m ");
							if (index < 10) {
								sb.append(" ");
							}
						} else {
							sb.append("\033[37m.\033[0m  ");
						}
					} else if (board[index] == Stone.RED) {
						sb.append("\033[1;31mX\033[0m  ");
					} else {
						sb.append("\033[1;34mO\033[0m  ");
					}
				}
				sb.append("\n");
			}
			return sb.toString();
		}
		@Override
		int play() {
			System.out.println("where to to put the next " + myColor + "?");
			var scanner = new Scanner(System.in, StandardCharsets.UTF_8);
			return Integer.parseInt(scanner.nextLine());
		}

	}
	public static class AlphaBetaPlayer extends DefaultPlayer {

		@Override
		public int play() {
			return findBestMove(Integer.MIN_VALUE, Integer.MAX_VALUE, board, myColor,5);
		}

		private int findBestMove(int alpha, int beta, Stone[] board, Stone color,int depth) {
			int bestMove = -1;
			for (int i = 0; i < board.length; i++) {
				if (board[i] == null) {
					Stone[] newBoard = play(board, i, color);
					int value = alphabeta(alpha, beta, newBoard, color.opponent(),depth);
					if (color == Stone.RED) {
						if (value > alpha) {
							alpha = value;
							bestMove = i;
						}
					} else {
						if (value < beta) {
							beta = value;
							bestMove = i;
						}
					}
					if (alpha >= beta) {
						break; // prune search tree
					}
				}
			}
			return bestMove;
		}

		private int alphabeta(int alpha, int beta, Stone[] board, Stone color,int depth) {
			if (isWinning(board, color)) {
				return Integer.MAX_VALUE;
			}else if(depth == 0){
				return evaluate(board,color);
			}
			else if (isWinning(board, color.opponent())) {
				return Integer.MIN_VALUE;
			} else if (isDraw(board)) {
				return 0;
			} else {
				for (int i = 0; i < board.length; i++) {
					if (board[i] == null) {
						int value = alphabeta(alpha, beta, play(board, i, color), color.opponent(),depth);
						if (color == Stone.RED) {
							alpha = Math.max(alpha, value);
						} else {
							beta = Math.min(beta, value);
						}
						if (alpha >= beta) {
							break;
						}
					}
				}
				return (color == Stone.RED) ? alpha : beta;
			}
		}
		private int evaluate(Stone[] board, Stone color) {
			int score = 0;
			for (int i = 0; i < board.length; i++) {
				if (board[i] == color) {
					score++;
				} else if (board[i] == color.opponent()) {
					score--;
				}
			}
			return score;
		}

		private boolean isDraw(Stone[] board) {
			for (int i = 0; i < board.length; i++) {
				if (board[i] == null) {
					return false;
				}
			}
			return !isWinning(board, Stone.RED) && !isWinning(board, Stone.BLUE);
		}



		private Stone[] play(Stone[] board, int move, Stone color) {
			Stone[] newBoard = Arrays.copyOf(board, board.length);
			newBoard[move] = color;
			return newBoard;
		}
	}
	public static class GreedyPlayer extends DefaultPlayer {

		@Override
		int play() {
			for (int c = 0; c < WIDTH; c++) {
				for (int r = 0; r < HEIGHT; r++) {
					var index = r * WIDTH + c;
					if (board[index] == null) {
						return index;
					}
				}
			}
			throw new IllegalStateException("cannot play at all");
		}
	}

}

package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.Edge;
import com.battle.heroes.army.programs.UnitTargetPathFinder;

import java.util.*;

/**
 * Поиск кратчайшего пути на поле 27x21 с препятствиями.
 * Используется BFS по клеткам (вес ребра = 1), допускается движение по диагонали.
 * Препятствиями считаются клетки, занятые живыми юнитами. Клетки старта и цели разрешены.
 * Если путь не найден — возвращается пустой список.
 *
 * Сложность BSF: O(W*H), память O(W*H), где W=27, H=21.
 */
public class UnitTargetPathFinderImpl implements UnitTargetPathFinder {

    private static final int WIDTH = 27;
    private static final int HEIGHT = 21;

    // 8 направлений (с диагоналями)
    private static final int[][] DIRS = {
            { 1,  0}, {-1,  0}, { 0,  1}, { 0, -1},
            { 1,  1}, { 1, -1}, {-1,  1}, {-1, -1}
    };

    @Override
    public List<Edge> getTargetPath(Unit attackUnit, Unit targetUnit, List<Unit> existingUnitList) {
        if (attackUnit == null || targetUnit == null) return Collections.emptyList();

        int sx = attackUnit.getxCoordinate();
        int sy = attackUnit.getyCoordinate();
        int tx = targetUnit.getxCoordinate();
        int ty = targetUnit.getyCoordinate();

        if (!inBounds(sx, sy) || !inBounds(tx, ty)) return Collections.emptyList();

        boolean[][] blocked = new boolean[WIDTH][HEIGHT];
        if (existingUnitList != null) {
            for (Unit u : existingUnitList) {
                if (u == null || !u.isAlive()) continue;
                int x = u.getxCoordinate();
                int y = u.getyCoordinate();
                if (inBounds(x, y)) blocked[x][y] = true;
            }
        }

        // старт и цель разрешаем (там находятся attackUnit/targetUnit)
        blocked[sx][sy] = false;
        blocked[tx][ty] = false;

        boolean[][] visited = new boolean[WIDTH][HEIGHT];
        Edge[][] parent = new Edge[WIDTH][HEIGHT];

        ArrayDeque<Edge> q = new ArrayDeque<>();
        q.add(new Edge(sx, sy));
        visited[sx][sy] = true;

        while (!q.isEmpty()) {
            Edge cur = q.poll();
            int cx = cur.getX();
            int cy = cur.getY();

            if (cx == tx && cy == ty) {
                return buildPath(parent, cur);
            }

            for (int[] d : DIRS) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (!inBounds(nx, ny)) continue;
                if (visited[nx][ny]) continue;
                if (blocked[nx][ny]) continue;

                visited[nx][ny] = true;
                parent[nx][ny] = cur;
                q.add(new Edge(nx, ny));
            }
        }

        return Collections.emptyList();
    }

    private static boolean inBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    private static List<Edge> buildPath(Edge[][] parent, Edge end) {
        List<Edge> path = new ArrayList<>();
        Edge cur = end;
        while (cur != null) {
            path.add(cur);
            cur = parent[cur.getX()][cur.getY()];
        }
        Collections.reverse(path);
        return path;
    }
}
package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.SuitableForAttackUnitsFinder;

import java.util.*;

/**
 * Отбор целей, подходящих для атаки.
 *
 * Цель считается "прикрытой", если в соседней клетке по координате y находится другой живой юнит этой же армии.
 * - при атаке левой армии: цель подходит, если нет юнита на y-1
 * - при атаке правой армии: цель подходит, если нет юнита на y+1
 *
 * Сложность: O(N) по количеству юнитов в списке.
 */
public class SuitableForAttackUnitsFinderImpl implements SuitableForAttackUnitsFinder {

    @Override
    public List<Unit> getSuitableUnits(List<List<Unit>> unitsByRow, boolean isLeftArmyTarget) {
        if (unitsByRow == null || unitsByRow.isEmpty()) {
            return Collections.emptyList();
        }

        int neighborDeltaY = isLeftArmyTarget ? -1 : 1;
        List<Unit> result = new ArrayList<>();

        for (List<Unit> row : unitsByRow) {
            if (row == null || row.isEmpty()) continue;

            // Для текущего ряда собираем координаты у всех живых юнитов,
            // чтобы за O(1) проверять наличие юнита рядом "прикрывающего" по y
            Set<Integer> aliveYs = new HashSet<>();
            for (Unit u : row) {
                if (u != null && u.isAlive()) {
                    aliveYs.add(u.getyCoordinate());
                }
            }

            for (Unit u : row) {
                if (u == null || !u.isAlive()) continue;

                int neighborY = u.getyCoordinate() + neighborDeltaY;
                if (!aliveYs.contains(neighborY)) {
                    result.add(u);
                }
            }
        }

        return result;
    }
}
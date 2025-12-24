package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.programs.PrintBattleLog;
import com.battle.heroes.army.programs.SimulateBattle;
import com.battle.heroes.army.Unit;

import java.util.*;

/**
 * Симуляция боя между двумя армиями.
 * Логика работы:
 * 1) Пока в обеих армиях есть живые юниты:
 *    - формируется список живых юнитов каждой армии и сортируется по убыванию атаки.
 *    - в рамках раунда ходы выполняются поочёрёдно (чередование сторон), при этом
 *       каждый юнит может совершить не более одного действия за раунд.
 *    - после действия выполняется логирование (если логгер задан) и удаление погибших юнитов.
 *    Дополнительно: если за раунд не было ни атак, ни перемещений, симуляция боя завершается досрочно,
 *    чтобы избежать бесконечного цикла в ситуациях, когда цели/пути недоступны.
 */
public class SimulateBattleImpl implements SimulateBattle {
    private PrintBattleLog printBattleLog; // логгер боя; может быть не задан

    @Override
    public void simulate(Army playerArmy, Army computerArmy) throws InterruptedException {
        if (playerArmy == null || computerArmy == null) return;

        List<Unit> playerUnits = new ArrayList<>(playerArmy.getUnits());
        List<Unit> computerUnits = new ArrayList<>(computerArmy.getUnits());

        while (hasAlive(playerUnits) && hasAlive(computerUnits)) {

            List<Unit> roundPlayer = sortedAliveByAttack(playerUnits);
            List<Unit> roundComputer = sortedAliveByAttack(computerUnits);

            Set<Unit> actedPlayer = new HashSet<>();
            Set<Unit> actedComputer = new HashSet<>();

            boolean computerTurn = true; // флаг очереди хода в текущем раунде
            boolean progressMadeInRound = false;

            while (true) {
                if (!hasAlive(playerUnits) || !hasAlive(computerUnits)) break;

                Unit attacker;
                if (computerTurn) {
                    attacker = nextNotActedAlive(roundComputer, actedComputer);
                    if (attacker == null) {
                        computerTurn = false;
                        if (nextNotActedAlive(roundPlayer, actedPlayer) == null) break;
                        continue;
                    }
                    actedComputer.add(attacker);
                } else {
                    attacker = nextNotActedAlive(roundPlayer, actedPlayer);
                    if (attacker == null) {
                        computerTurn = true;
                        if (nextNotActedAlive(roundComputer, actedComputer) == null) break;
                        continue;
                    }
                    actedPlayer.add(attacker);
                }

                // Если юнит умер до своего хода — он пропускается.
                if (attacker == null || !attacker.isAlive()) {
                    computerTurn = !computerTurn;
                    continue;
                }

                int beforeX = attacker.getxCoordinate();
                int beforeY = attacker.getyCoordinate();

                Unit target = attacker.getProgram().attack();

                int afterX = attacker.getxCoordinate();
                int afterY = attacker.getyCoordinate();

                if (target != null && printBattleLog != null) {
                    printBattleLog.printBattleLog(attacker, target);
                }

               // Прогресс в раунде - это либо атака, либо хотя бы одно перемещение.
                if (target != null || beforeX != afterX || beforeY != afterY) {
                    progressMadeInRound = true;
                }

                // Обновление состояния армий: удаляем погибших и пересчитываем очереди.
                cleanupDead(playerUnits);
                cleanupDead(computerUnits);

                actedPlayer.removeIf(u -> u == null || !u.isAlive());
                actedComputer.removeIf(u -> u == null || !u.isAlive());

                roundPlayer = sortedAliveByAttack(playerUnits);
                roundComputer = sortedAliveByAttack(computerUnits);

                computerTurn = !computerTurn;

                // Если в этом раунде вообще никто больше не может ходить — заканчиваем раунд
                if (nextNotActedAlive(roundPlayer, actedPlayer) == null
                        && nextNotActedAlive(roundComputer, actedComputer) == null) {
                    break; // раунд завершён
                }
            }

            if (!progressMadeInRound) {
                // Если в раунде не было прогресса (атаки или перемещения), завершаем бой во избежание бесконечного цикла
                break;
            }
        }
    }

    private static boolean hasAlive(List<Unit> units) {
        for (Unit u : units) {
            if (u != null && u.isAlive()) return true;
        }
        return false;
    }

    private static List<Unit> sortedAliveByAttack(List<Unit> units) {
        List<Unit> alive = new ArrayList<>();
        for (Unit u : units) {
            if (u != null && u.isAlive()) alive.add(u);
        }
        alive.sort(Comparator.comparingInt(Unit::getBaseAttack).reversed());
        return alive;
    }

    private static Unit nextNotActedAlive(List<Unit> roundList, Set<Unit> acted) {
        for (Unit u : roundList) {
            if (u != null && u.isAlive() && !acted.contains(u)) return u;
        }
        return null;
    }

    private static void cleanupDead(List<Unit> units) {
        units.removeIf(u -> u == null || !u.isAlive() || u.getHealth() <= 0);
    }
}
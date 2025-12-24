package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.GeneratePreset;

import java.util.*;

/**
 * Реализация генерации пресета армии компьютера.
 * Алгоритм:
 * 1) Выбираем лучшие шаблоны юнитов по типам (если в списке несколько юнитов одного типа).
 * 2) Сортируем шаблоны по эффективности (атака/стоимость, здоровье/стоимость), рандомный тай-брейк при равенстве.
 * 3) Генерируем список случайных свободных позиций в 3 колонках слева.
 * 4) Жадно добавляем юниты по шаблонам, пока хватает очков и не превышен лимит на типы.
 * 5) Координаты выбираются случайно в пределах 3 колонок без пересечений.
 */
public class GeneratePresetImpl implements GeneratePreset {

    private static final int MAX_UNITS_PER_TYPE = 11;

    // Пресет компьютера занимает 3 колонки слева (x = 0,1,2) и высоту поля 21 (y = 0..20)
    private static final int PRESET_X_FROM = 0;
    private static final int PRESET_X_TO = 2;      // inclusive
    private static final int FIELD_HEIGHT = 21;    // y = 0..20

    @Override
    public Army generate(List<Unit> unitList, int maxPoints) {
        if (unitList == null || unitList.isEmpty() || maxPoints <= 0) {
            Army empty = new Army(new ArrayList<>());
            empty.setPoints(0);
            return empty;
        }

        // В unitList обычно по одному юниту каждого типа. На всякий случай выбираю лучший шаблон на тип
        // по метрике (атака/стоимость), затем (здоровье/стоимость).
        Map<String, Unit> bestTemplateByType = new HashMap<>();
        for (Unit u : unitList) {
            if (u == null) continue;
            bestTemplateByType.merge(u.getUnitType(), u,
                    (a, b) -> compareTemplates(b, a) > 0 ? b : a);
        }

        List<Unit> templates = new ArrayList<>(bestTemplateByType.values());
        if (templates.isEmpty()) {
            Army empty = new Army(new ArrayList<>());
            empty.setPoints(0);
            return empty;
        }

        // Сортируем шаблоны по эффективности (убывание). При полном равенстве добавляем рандом,
        // чтобы пресеты не выглядели одинаково.
        Random rnd = new Random(System.nanoTime());
        templates.sort((a, b) -> {
            int cmp = compareTemplates(a, b);
            if (cmp != 0) return cmp;
            return rnd.nextInt(3) - 1;
        });

        // Список всех свободных позиций в пресете (без пересечений)
        List<int[]> freePositions = new ArrayList<>();
        for (int x = PRESET_X_FROM; x <= PRESET_X_TO; x++) {
            for (int y = 0; y < FIELD_HEIGHT; y++) {
                freePositions.add(new int[]{x, y});
            }
        }
        Collections.shuffle(freePositions, rnd);
        int posIdx = 0;

        // Жадно добавляем юниты по шаблонам, пока хватает очков и не превышен лимит на типы
        Map<String, Integer> typeCount = new HashMap<>();
        List<Unit> result = new ArrayList<>();
        int currentPoints = 0;

        boolean added;
        do {
            added = false;

            for (Unit template : templates) {
                if (template == null) continue;
                String type = template.getUnitType();
                int cnt = typeCount.getOrDefault(type, 0);
                if (cnt >= MAX_UNITS_PER_TYPE) continue;

                int cost = template.getCost();
                if (currentPoints + cost > maxPoints) continue;

                if (posIdx >= freePositions.size()) {
                    // мест больше нет
                    break;
                }

                int[] pos = freePositions.get(posIdx++);
                int x = pos[0];
                int y = pos[1];

                String name = type + " " + (cnt + 1);

                Unit unit = new Unit(
                        name,
                        type,
                        template.getHealth(),
                        template.getBaseAttack(),
                        cost,
                        template.getAttackType(),
                        template.getAttackBonuses(),
                        template.getDefenceBonuses(),
                        x,
                        y
                );

                result.add(unit);
                currentPoints += cost;
                typeCount.put(type, cnt + 1);
                added = true;
            }
        } while (added);

        Army army = new Army(result);
        army.setPoints(currentPoints);
        return army;
    }

    /** Сравнение шаблонов юнитов по эффективности:
     * 1) Сравнение по (атака/стоимость) - убывание
     * 2) При равенстве сравнение по (здоровье/стоимость) - убывание
     */
    private static int compareTemplates(Unit a, Unit b) {
        double aAtk = ratio(a.getBaseAttack(), a.getCost());
        double bAtk = ratio(b.getBaseAttack(), b.getCost());
        int cmpAtk = Double.compare(bAtk, aAtk);
        if (cmpAtk != 0) return cmpAtk;

        double aHp = ratio(a.getHealth(), a.getCost());
        double bHp = ratio(b.getHealth(), b.getCost());
        int cmpHp = Double.compare(bHp, aHp);
        if (cmpHp != 0) return cmpHp;

        return 0;
    }

    private static double ratio(int value, int cost) {
        if (cost <= 0) return 0.0;
        return (double) value / (double) cost;
    }
}
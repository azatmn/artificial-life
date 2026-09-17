// Сборка отчёта по лабораторной работе № 1 в формате .docx
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, ImageRun, Table, TableRow, TableCell,
  AlignmentType, HeadingLevel, WidthType, BorderStyle, ShadingType, PageOrientation,
  Footer, PageNumber, TableOfContents, LevelFormat, PageBreak, VerticalAlign,
} = require('docx');

const PROJECT = '/Users/azatminyazov/Workspace/university java/1 work';
const OUT = process.env.OUT || path.join(PROJECT, 'docs/report/Отчет_ЛР1_Минязов.docx');

const FONT = 'Times New Roman';
const MONO = 'Courier New';
const MM = 56.7; // twips в миллиметре

// ---------- базовые элементы ----------

function runs(text, opts = {}) {
  // **жирный** внутри строки
  return text.split(/(\*\*[^*]+\*\*)/).filter(Boolean).map(part =>
    part.startsWith('**')
      ? new TextRun({ text: part.slice(2, -2), bold: true, font: opts.font, size: opts.size })
      : new TextRun({ text: part, font: opts.font, size: opts.size, italics: opts.italics, bold: opts.bold }));
}

function p(text, opts = {}) {
  return new Paragraph({
    children: runs(text, opts),
    alignment: opts.align ?? AlignmentType.JUSTIFIED,
    indent: opts.noIndent ? undefined : { firstLine: 709 },
    spacing: { line: 360, before: opts.before ?? 0, after: opts.after ?? 0 },
    keepNext: opts.keepNext,
  });
}

function center(text, opts = {}) {
  return p(text, { ...opts, align: AlignmentType.CENTER, noIndent: true });
}

function h1(text, pageBreak = true) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    children: [new TextRun(text)],
    pageBreakBefore: pageBreak,
    spacing: { before: 0, after: 240, line: 360 },
    indent: { firstLine: 709 },
    keepNext: true,
  });
}

function h2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    children: [new TextRun(text)],
    spacing: { before: 240, after: 120, line: 360 },
    indent: { firstLine: 709 },
    keepNext: true,
  });
}

function bullet(text) {
  return new Paragraph({
    children: runs(text),
    numbering: { reference: 'dash', level: 0 },
    alignment: AlignmentType.JUSTIFIED,
    spacing: { line: 360 },
  });
}

let figureNo = 0;
let tableNo = 0;

function pngSize(file) {
  const buf = fs.readFileSync(file);
  return { width: buf.readUInt32BE(16), height: buf.readUInt32BE(20) };
}

// maxW, maxH — в пикселях при 96 dpi (1 мм ≈ 3,78 px)
function figure(file, caption, maxW, maxH) {
  const { width, height } = pngSize(file);
  const scale = Math.min(maxW / width, maxH / height, 1);
  figureNo++;
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 120, after: 60 },
      keepNext: true,
      children: [new ImageRun({
        type: 'png',
        data: fs.readFileSync(file),
        transformation: { width: Math.round(width * scale), height: Math.round(height * scale) },
        altText: { title: caption, description: caption, name: path.basename(file) },
      })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 200, line: 276 },
      children: [new TextRun({ text: `Рисунок ${figureNo} — ${caption}`, size: 24 })],
    }),
  ];
}

const THIN = { style: BorderStyle.SINGLE, size: 4, color: '000000' };
const BORDERS = { top: THIN, bottom: THIN, left: THIN, right: THIN };

function table(caption, header, rows, widthsMm) {
  tableNo++;
  const widths = widthsMm.map(w => Math.round(w * MM));
  const total = widths.reduce((a, b) => a + b, 0);
  const cell = (text, i, isHeader) => new TableCell({
    width: { size: widths[i], type: WidthType.DXA },
    borders: BORDERS,
    shading: isHeader ? { type: ShadingType.CLEAR, fill: 'E7E6E6', color: 'auto' } : undefined,
    margins: { top: 40, bottom: 40, left: 90, right: 90 },
    verticalAlign: VerticalAlign.CENTER,
    children: [new Paragraph({
      spacing: { line: 240 },
      alignment: isHeader ? AlignmentType.CENTER : AlignmentType.LEFT,
      children: runs(text, { size: 24, bold: isHeader }),
    })],
  });
  return [
    new Paragraph({
      spacing: { before: 200, after: 60 },
      keepNext: true,
      children: [new TextRun({ text: `Таблица ${tableNo} — ${caption}`, size: 24 })],
    }),
    new Table({
      width: { size: total, type: WidthType.DXA },
      columnWidths: widths,
      rows: [
        new TableRow({ tableHeader: true, children: header.map((t, i) => cell(t, i, true)) }),
        ...rows.map(r => new TableRow({ cantSplit: true, children: r.map((t, i) => cell(t, i, false)) })),
      ],
    }),
    new Paragraph({ spacing: { after: 120 }, children: [] }),
  ];
}

function codeFile(relPath) {
  const text = fs.readFileSync(path.join(PROJECT, relPath), 'utf8').replace(/\t/g, '    ');
  const lines = text.replace(/\n$/, '').split('\n');
  return [
    new Paragraph({
      spacing: { before: 240, after: 80 },
      keepNext: true,
      children: [new TextRun({ text: `Файл ${relPath}`, bold: true, size: 24 })],
    }),
    ...lines.map(line => new Paragraph({
      spacing: { line: 240, before: 0, after: 0 },
      children: [new TextRun({ text: line.length ? line : ' ', font: MONO, size: 17 })],
    })),
  ];
}

// ---------- размеры страниц ----------

const MARGINS = { top: 20 * MM, bottom: 20 * MM, left: 30 * MM, right: 15 * MM };
const A4 = { width: 11906, height: 16838 };
const PORTRAIT_W = 620;   // 165 мм текста
const PORTRAIT_H = 860;
const LAND_W = 940;       // 252 мм текста
const LAND_H = 560;

const footer = () => ({
  default: new Footer({
    children: [new Paragraph({
      alignment: AlignmentType.CENTER,
      children: [new TextRun({ children: [PageNumber.CURRENT], size: 24 })],
    })],
  }),
});

const portrait = { page: { size: A4, margin: MARGINS } };
const landscape = { page: { size: { ...A4, orientation: PageOrientation.LANDSCAPE }, margin: MARGINS } };

const shot = name => path.join(PROJECT, 'docs/screenshots', name);
const uml = name => path.join(PROJECT, 'docs/uml', name);

// ---------- титульный лист ----------

function signatureTable(rows) {
  const none = { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' };
  const noBorders = { top: none, bottom: none, left: none, right: none };
  const widths = [70, 45, 50].map(w => Math.round(w * MM));
  return new Table({
    width: { size: widths.reduce((a, b) => a + b, 0), type: WidthType.DXA },
    columnWidths: widths,
    rows: rows.map(r => new TableRow({
      children: r.map((t, i) => new TableCell({
        width: { size: widths[i], type: WidthType.DXA },
        borders: noBorders,
        children: [new Paragraph({
          alignment: i === 2 ? AlignmentType.RIGHT : AlignmentType.LEFT,
          spacing: { line: 360 },
          children: [new TextRun({ text: t })],
        })],
      })),
    })),
  });
}

const titlePage = [
  center('Министерство науки и высшего образования Российской Федерации'),
  center('Санкт-Петербургский политехнический университет Петра Великого'),
  center('Институт компьютерных наук и кибербезопасности'),
  new Paragraph({ spacing: { before: 1500 }, children: [] }),
  center('**ОТЧЁТ**', { size: 32 }),
  center('о лабораторной работе № 1'),
  new Paragraph({ spacing: { before: 200 }, children: [] }),
  center('«Искусственная жизнь» — программа, использующая логических агентов', { bold: true }),
  new Paragraph({ spacing: { before: 200 }, children: [] }),
  center('по дисциплине «Объектно-ориентированное программирование на языке Java»'),
  new Paragraph({ spacing: { before: 1500 }, children: [] }),
  signatureTable([
    ['Выполнил', '', ''],
    ['студент гр. ______________', '____________', 'А. А. Минязов'],
    ['', '', ''],
    ['Проверил', '', ''],
    ['старший преподаватель', '____________', 'Д. С. Хасанов'],
  ]),
  new Paragraph({ spacing: { before: 500 }, children: [] }),
  center('Санкт-Петербург'),
  center('2026'),
];

// ---------- содержание и главы ----------

const tocPage = [
  center('**СОДЕРЖАНИЕ**', { after: 240 }),
  new TableOfContents('Содержание', { hyperlink: true, headingStyleRange: '1-2' }),
];

const chapter1 = [
  h1('1 Постановка задачи'),
  p('Требуется разработать программу, реализующую алгоритм искусственной жизни. Алгоритм моделирует пищевую цепочку для логических агентов трёх видов: «хищники», «травоядные» и «растения». Агенты помещены в искусственную среду, представленную двумерным массивом ячеек. В ячейке может находиться один из агентов, либо ячейка может быть пустой.'),
  p('Каждый агент имеет некоторое количество единиц энергии. Энергия хищника и травоядного уменьшается с каждым шагом игры. Чтобы увеличить энергию, хищник поедает травоядного, а травоядное поедает растение; при этом энергия агента увеличивается на заданное число. Если энергия агента равна нулю, он погибает. Если энергия превышает порог, появляется новый агент, который забирает часть энергии.'),
  p('Травоядные и хищники обладают зрением и могут перемещаться. Область видимости составляет 5×5 клеток, агент находится в центре. Действия агентов основаны на логических выражениях.'),
  p('Необходимо создать классы окружающей среды, растения, травоядного, хищника и другие необходимые классы; класс агента должен включать данные и методы действий: движение, поедание, появление, исчезновение. Параметры программы нужно подобрать так, чтобы жизнь не прекращалась. Предварительный вариант программы — консольное приложение, окончательный — оконное приложение.'),
  h2('1.1 Цель работы'),
  p('Цель работы — создать симуляцию экосистемы, в которой автономные логические агенты взаимодействуют друг с другом в рамках пищевой цепочки по строгим алгоритмическим правилам, и освоить на её примере основные принципы объектно-ориентированного программирования на языке Java: абстракцию, инкапсуляцию, наследование и полиморфизм.'),
  h2('1.2 Декомпозиция задачи'),
  p('Задача разбита на четыре подсистемы:'),
  bullet('**пространственная модель (среда)** — двумерная сетка W×H, в клетке не более одного агента;'),
  bullet('**сущностная модель (агенты)** — три вида агентов с общим предком и собственным поведением;'),
  bullet('**физическая модель (энергия и метаболизм)** — расход, пополнение, гибель и размножение делением;'),
  bullet('**когнитивная модель (зрение и логика)** — выбор действия по содержимому области 5×5.'),
];

const chapter2 = [
  h1('2 Описание модели'),
  h2('2.1 Среда'),
  p('Среда — прямоугольное поле из клеток со стенами по краям (по умолчанию 100×60). Поле хранится в двумерном массиве Agent[][]: в клетке находится ссылка на агента или пустое значение null. Правило «в клетке не более одного агента» проверяется при каждом размещении и перемещении; координаты агента изменяет только поле, поэтому данные поля и агента всегда согласованы.'),
  h2('2.2 Агенты'),
  bullet('**Растение** не перемещается. Каждый шаг получает энергию «от солнца» (не выше заданного максимума) и при избытке энергии разрастается в соседнюю свободную клетку.'),
  bullet('**Травоядное** перемещается, видит область 5×5, питается растениями и убегает от хищников.'),
  bullet('**Хищник** перемещается, видит область 5×5 и охотится на травоядных.'),
  p('Животные ходят на одну клетку за шаг в любом из восьми направлений, в том числе по диагонали.'),
  h2('2.3 Энергия, гибель и размножение'),
  bullet('**Метаболизм:** каждый шаг энергия животного уменьшается на расход своего вида; у растения расход равен нулю.'),
  bullet('**Питание:** съев добычу, животное получает фиксированную энергию своего вида и занимает клетку добычи.'),
  bullet('**Гибель:** если энергия животного стала равна нулю, оно погибает и удаляется с поля. Растение погибает, когда его съедают.'),
  bullet('**Размножение (деление):** если энергия агента строго больше порога его вида и рядом есть свободная клетка, в случайной свободной соседней клетке появляется потомок того же вида, получающий половину энергии родителя. Суммарная энергия при делении сохраняется.'),
  h2('2.4 Логика поведения'),
  p('Поведение животных задано логическими выражениями. Травоядное:'),
  bullet('ЕСЛИ видит хищника — делает шаг в свободную клетку, наиболее удалённую от ближайшего хищника;'),
  bullet('ИНАЧЕ ЕСЛИ видит растение — делает шаг к ближайшему растению и съедает его, если шаг приходится на клетку с растением;'),
  bullet('ИНАЧЕ — делает шаг в случайную свободную соседнюю клетку.'),
  p('Хищник:'),
  bullet('ЕСЛИ видит травоядное — делает шаг к ближайшему травоядному и съедает его, если шаг приходится на его клетку;'),
  bullet('ИНАЧЕ — делает шаг в случайную свободную соседнюю клетку.'),
  p('Перед выбором действия животное тратит энергию за ход, после действия — пытается размножиться. Если приблизиться к цели или отдалиться от угрозы нельзя, животное остаётся на месте. При нескольких одинаково подходящих вариантах выбор случайный.'),
  h2('2.5 Шаг времени'),
  p('За один шаг каждый агент, живой на начало шага, делает один ход. Порядок ходов каждый раз перемешивается случайным образом, чтобы ни один агент не получал постоянного преимущества. Агент, съеденный до своей очереди, не ходит; потомок, родившийся на этом шаге, начинает ходить со следующего. Все случайные решения принимаются одним генератором случайных чисел, который создаётся из зерна (seed) в параметрах, поэтому при одинаковом seed ход симуляции полностью повторяется.'),
  h2('2.6 Параметры и их подбор'),
  p('Параметры с наугад выбранными значениями приводили к колебаниям «бум и крах»: травоядные быстро размножались и выедали растения, хищники размножались ещё быстрее и истребляли травоядных, после чего один из видов вымирал на 39–165-м шаге.'),
  p('Параметры подобраны автоматическим перебором. Сначала проверено 600 случайных наборов (по 3 прогона на 1500 шагов); затем 12 лучших наборов в течение шести раундов улучшались небольшими случайными изменениями (по 6 прогонов на 3000 шагов); пять финалистов проверены на 20 новых начальных расстановках по 10 000 шагов. Лучший набор (таблица 1) сохранил все три вида во всех 20 прогонах.'),
  ...table('Параметры по умолчанию',
    ['Параметр', 'Растение', 'Травоядное', 'Хищник'],
    [
      ['Количество в начале', '2000', '270', '30'],
      ['Начальная энергия', '3', '13', '52'],
      ['Изменение энергии за шаг', '+3 (не выше 12)', '−1', '−2'],
      ['Энергия от пищи', '—', '+14', '+13'],
      ['Порог размножения', '9', '26', '103'],
    ],
    [65, 35, 32, 33]),
  p('Размер поля — 100×60 клеток, seed по умолчанию — 42. Найденные значения сдерживают «бум» хищников (для деления хищнику нужно съесть около восьми травоядных, а без пищи он быстро теряет энергию) и ускоряют восстановление растений, поэтому травоядные не успевают выесть их полностью.'),
  p('Итоговые параметры дополнительно проверены на 40 начальных расстановках, не участвовавших в подборе: во всех 40 прогонах по 10 000 шагов все три вида сохранились; наименьшая численность травоядных — 179, хищников — 30. Проверка устойчивости (каждый параметр изменялся на ±20 %, по 8 прогонов на 5000 шагов) показала, что баланс чувствителен к трём параметрам (таблица 2); при остальных изменениях сохранялись все виды.'),
  ...table('Параметры, к изменению которых чувствителен баланс',
    ['Изменение', 'Прогонов без вымирания'],
    [
      ['Порог размножения растения −20 % (9 → 7)', '3 из 8'],
      ['Энергия хищника от травоядного +20 % (13 → 16)', '6 из 8'],
      ['Прирост энергии растения +20 % (3 → 4)', '7 из 8'],
      ['Остальные параметры ±20 %', '8 из 8'],
    ],
    [115, 50]),
  p('Вывод: опасна не только «трудная», но и слишком «лёгкая» жизнь — избыток пищи усиливает колебания численности и приводит к вымиранию.'),
];

const chapter3 = [
  h1('3 Проектирование'),
  p('Система описана пятью видами UML-диаграмм: прецедентов, деятельности, классов, последовательности и компонентов. Диаграммы построены в PlantUML; их исходные тексты хранятся в каталоге docs/uml проекта.'),
  h2('3.1 Диаграмма прецедентов'),
  p('Диаграмма прецедентов (рисунок 1) показывает, что может делать пользователь: запускать и приостанавливать симуляцию, выполнять один шаг или (в консоли) N шагов, сбрасывать поле, менять скорость, настраивать параметры и наблюдать поле, счётчики и график. Настройка параметров всегда включает их проверку; при ошибках пользователю показываются сообщения и подсвечиваются неверные поля.'),
  ...figure(uml('usecase.png'), 'Диаграмма прецедентов', PORTRAIT_W, 700),
  h2('3.2 Диаграммы деятельности'),
  p('Алгоритм хода каждого вида агентов показан отдельной диаграммой деятельности (рисунки 2–4). Диаграммы соответствуют методам act() классов Plant, Herbivore и Predator.'),
  ...figure(uml('activity_plant.png'), 'Диаграмма деятельности: ход растения', 420, 520),
  ...figure(uml('activity_herbivore.png'), 'Диаграмма деятельности: ход травоядного', PORTRAIT_W, PORTRAIT_H),
  ...figure(uml('activity_predator.png'), 'Диаграмма деятельности: ход хищника', PORTRAIT_W, PORTRAIT_H),
  h2('3.3 Диаграммы классов'),
  p('Классы модели показаны на рисунке 5. Абстрактный класс Agent хранит общие для всех агентов данные (позицию, энергию, признак жизни) и общие действия (изменение энергии, гибель, размножение делением), а метод хода act() и способ создания потомка объявляет абстрактными. От него наследуются растение Plant и абстрактное животное Animal, которое добавляет зрение 5×5, перемещение, поедание и расход энергии. Травоядное Herbivore и хищник Predator реализуют только собственную логику и определение пищи. Класс World реализует интерфейс ReadOnlyWorld, через который интерфейс пользователя получает поле только для чтения. Параметры собраны в неизменяемой записи Settings, которая проверяет свои значения при создании.'),
  ...figure(uml('class_model.png'), 'Диаграмма классов модели (пакет life.model)', PORTRAIT_W, PORTRAIT_H),
];

const landscapePart = [
  p('Классы управления и интерфейса показаны на рисунке 6. Оконный интерфейс (пакет life.view.gui) обращается к модели через контроллер SimulationController, консольная версия (пакет life.view.console) — напрямую к Simulation.', { before: 0 }),
  ...figure(uml('class_app.png'), 'Диаграмма классов управления и интерфейса', LAND_W, LAND_H - 40),
  h2('3.4 Диаграмма последовательности'),
  p('На рисунке 7 показано взаимодействие объектов за один кадр окна: таймер вызывает окно, окно передаёт время контроллеру, контроллер выполняет нужное число шагов симуляции, симуляция вызывает ход каждого агента, агент запрашивает у поля содержимое области 5×5 и перемещается, после чего окно перерисовывает поле и график.'),
  ...figure(uml('sequence_step.png'), 'Диаграмма последовательности одного кадра окна', LAND_W, LAND_H - 110),
];

const chapter3end = [
  h2('3.5 Диаграмма компонентов'),
  p('Программа оформлена как модуль Java life и разделена на пакеты по схеме «модель — представление — контроллер» (рисунок 8). Модель не зависит от интерфейса; оконная версия зависит от библиотеки JavaFX.'),
  ...figure(uml('component.png'), 'Диаграмма компонентов', 520, 520),
];

const chapter4 = [
  h1('4 Реализация'),
  h2('4.1 Средства разработки'),
  bullet('язык Java 25 (JDK Temurin 25);'),
  bullet('библиотека пользовательского интерфейса JavaFX 25.0.4;'),
  bullet('система сборки Maven, среда разработки IntelliJ IDEA;'),
  bullet('библиотека тестирования JUnit 6.1.3;'),
  bullet('система контроля версий Git.'),
  h2('4.2 Структура программы'),
  ...table('Пакеты программы',
    ['Пакет', 'Назначение'],
    [
      ['life', 'Launcher — точка входа оконной версии'],
      ['life.model', 'Settings, World, ReadOnlyWorld, Position, Agent, Plant, Animal, Herbivore, Predator, Simulation, Statistics, InvalidSettingsException — модель'],
      ['life.controller', 'SimulationController (старт, пауза, шаг, сброс, скорость), SettingsForm (разбор полей окна параметров), SpeedScale (шкала скорости)'],
      ['life.view.console', 'ConsoleMain, ConsoleApp, CommandParser, Command, ConsoleRenderer — консольная версия'],
      ['life.view.gui', 'LifeApp, WorldCanvas, PopulationChart, SettingsDialog — оконная версия'],
      ['life.util', 'SafeText — безопасный вывод введённого пользователем текста в сообщениях'],
    ],
    [42, 123]),
  h2('4.3 Применение принципов ООП'),
  ...table('Принципы ООП в программе',
    ['Принцип', 'Реализация'],
    [
      ['Абстракция', 'Абстрактные классы Agent и Animal описывают «агента вообще» и «животное вообще»; создать их объекты нельзя, каждый вид обязан реализовать метод хода act()'],
      ['Наследование', 'Agent → Plant; Agent → Animal → Herbivore, Predator. Общие данные и действия (энергия, деление, зрение, перемещение) реализованы один раз в предках'],
      ['Полиморфизм', 'Simulation.step() вызывает agent.act(world, random) для переменной типа Agent; выполняется версия метода конкретного вида'],
      ['Инкапсуляция', 'Поля энергии и позиции закрыты (private) и изменяются только методами; перемещать агентов может только World; интерфейс получает поле только для чтения (ReadOnlyWorld); Settings неизменяем и проверяет значения при создании'],
    ],
    [35, 130]),
  h2('4.4 Консольная версия'),
  p('Консольная версия выводит поле символами (P — растение, T — травоядное, X — хищник, «.» — пустая клетка) и строку со счётчиками, после чего читает команды пользователя (таблица 5). Любой некорректный ввод (буквы, дробные и отрицательные числа, слишком большие числа, управляющие символы) не приводит к аварийному завершению: программа выводит понятное сообщение и продолжает работу. При выполнении долгой команды раз в секунду печатается прогресс; если на поле не осталось агентов, шаги прекращаются с сообщением «Жизнь прекратилась».'),
  ...table('Команды консольной версии',
    ['Ввод', 'Действие'],
    [
      ['Enter', 'один шаг'],
      ['число N', 'N шагов подряд (от 1 до 100 000)'],
      ['h, ?, помощь', 'справка'],
      ['q, выход, конец ввода', 'выход из программы'],
    ],
    [55, 110]),
  h2('4.5 Оконная версия'),
  p('Главное окно (класс LifeApp) содержит поле в центре, панель управления справа и график численности снизу. Панель управления включает кнопки «Старт/Пауза», «Шаг», «Сброс», «Параметры…», слайдер скорости (от 1 до 500 шагов в секунду, логарифмическая шкала) и счётчики с цветовыми обозначениями видов.'),
  p('Игровой цикл реализован таймером JavaFX AnimationTimer, который вызывается при каждой перерисовке экрана (около 60 раз в секунду). Контроллер накапливает прошедшее время, умноженное на скорость, и выполняет столько целых шагов, сколько накопилось. За один кадр выполняется не более 20 шагов, поэтому окно не зависает; время, проведённое на паузе, не превращается в шаги. График обновляется не чаще четырёх раз в секунду и хранит последние 1000 точек.'),
  p('Окно параметров содержит поля для всех 18 параметров, кнопки «Случайный seed» и «По умолчанию». Разбор введённых значений выполняет класс SettingsForm; при ошибках окно не закрывается, неверные поля подсвечиваются, под формой выводится список всех проблем.'),
  h2('4.6 Тестирование'),
  p('Программа покрыта 417 автоматическими тестами JUnit, которые запускаются в случайном порядке (таблица 6). Тесты написаны до реализации соответствующего кода; для каждой проверки «действие выполнено» есть зеркальная проверка «лишнего не сделано». Логика кнопок проверена без открытия окна: время передаётся контроллеру вручную.'),
  ...table('Автоматические тесты',
    ['Что проверяется', 'Тестов'],
    [
      ['Параметры и их проверка (Settings)', '84'],
      ['Поле, координаты, зрение (World, Position)', '58'],
      ['Агенты: энергия, деление, рост, поведение (Agent, Plant, Animal, Herbivore, Predator)', '88'],
      ['Шаг симуляции и статистика (Simulation, Statistics)', '22'],
      ['Сохранение всех видов на 5000 шагах при параметрах по умолчанию (BalanceTest)', '5'],
      ['Консольная версия: команды, вывод, цикл ввода', '81'],
      ['Контроллер окна, форма параметров, шкала скорости, SafeText', '79'],
      ['**Всего**', '**417**'],
    ],
    [140, 25]),
  p('Качество тестов проверено мутационным тестированием: в код намеренно вносились небольшие ошибки (замена «>» на «>=», удаление вызова, перестановка аргументов и т. п.), после чего запускались тесты. Всего внесено 189 мутаций; мутации, не обнаруженные тестами с первого раза, указали на пропущенные случаи (например, квадратные поля не отличали ширину от высоты), и для них были добавлены тесты. Оконная версия дополнительно проверена живым запуском: нажатием кнопок, вводом ошибочных параметров и закрытием окна.'),
];

const chapter5 = [
  h1('5 Результаты работы'),
  h2('5.1 Консольная версия'),
  p('На рисунке 9 показан запуск консольной версии: справка по командам, строка счётчиков и поле на шаге 0. На рисунке 10 — реакция на неизвестную команду «abc» и выполнение шага по Enter. На рисунке 11 — выполнение 10 шагов одной командой; видно быстрое размножение травоядных на первых шагах.'),
  ...figure(shot('console_1_start.png'), 'Консольная версия: справка и начальное поле', PORTRAIT_W, 640),
  ...figure(shot('console_2_error.png'), 'Консольная версия: ошибка ввода и шаг', PORTRAIT_W, 640),
  ...figure(shot('console_3_steps.png'), 'Консольная версия: выполнение 10 шагов', PORTRAIT_W, 640),
  h2('5.2 Оконная версия'),
  p('На рисунке 12 показано окно программы при запуске, на рисунке 13 — через 8 секунд работы (159 шагов). На графике видны колебания численности видов, при которых все три вида сохраняются. На рисунке 14 показано окно параметров после ввода ошибочных значений: окно не закрылось, неверные поля подсвечены, под формой перечислены все ошибки.'),
  ...figure(shot('gui_1_start.png'), 'Оконная версия при запуске', PORTRAIT_W, 640),
  ...figure(shot('gui_2_running.png'), 'Оконная версия через 8 секунд работы', PORTRAIT_W, 640),
  ...figure(shot('gui_3_settings_errors.png'), 'Окно параметров с ошибочным вводом', 420, 620),
];

const conclusion = [
  h1('Заключение'),
  p('В ходе лабораторной работы разработана программа «Искусственная жизнь», моделирующая пищевую цепочку из растений, травоядных и хищников на клеточном поле. Агенты обладают зрением 5×5, действуют по логическим правилам, тратят и пополняют энергию, погибают и размножаются делением.'),
  p('Программа построена на принципах ООП: абстрактные классы Agent и Animal задают общее поведение, конкретные виды наследуют его и реализуют собственную логику, симуляция работает с агентами полиморфно, данные агентов и параметры инкапсулированы. Код разделён на модель, контроллер и представление; реализованы консольная (предварительная) и оконная (окончательная) версии.'),
  p('Параметры подобраны автоматическим перебором так, чтобы жизнь не прекращалась: при параметрах по умолчанию все три вида сохранились во всех 40 проверочных прогонах по 10 000 шагов. Корректность программы подтверждена 417 автоматическими тестами, мутационным тестированием и проверкой работы интерфейса.'),
];

const appendixFiles = [
  'pom.xml',
  'src/main/java/module-info.java',
  'src/main/java/life/Launcher.java',
  'src/main/java/life/model/Settings.java',
  'src/main/java/life/model/InvalidSettingsException.java',
  'src/main/java/life/model/Position.java',
  'src/main/java/life/model/ReadOnlyWorld.java',
  'src/main/java/life/model/World.java',
  'src/main/java/life/model/Agent.java',
  'src/main/java/life/model/Plant.java',
  'src/main/java/life/model/Animal.java',
  'src/main/java/life/model/Herbivore.java',
  'src/main/java/life/model/Predator.java',
  'src/main/java/life/model/Statistics.java',
  'src/main/java/life/model/Simulation.java',
  'src/main/java/life/controller/SimulationController.java',
  'src/main/java/life/controller/SettingsForm.java',
  'src/main/java/life/controller/SpeedScale.java',
  'src/main/java/life/util/SafeText.java',
  'src/main/java/life/view/console/ConsoleMain.java',
  'src/main/java/life/view/console/ConsoleApp.java',
  'src/main/java/life/view/console/Command.java',
  'src/main/java/life/view/console/CommandParser.java',
  'src/main/java/life/view/console/ConsoleRenderer.java',
  'src/main/java/life/view/gui/LifeApp.java',
  'src/main/java/life/view/gui/WorldCanvas.java',
  'src/main/java/life/view/gui/PopulationChart.java',
  'src/main/java/life/view/gui/SettingsDialog.java',
  'src/main/resources/life/view/gui/app.css',
];

const appendix = [
  h1('Приложение А. Текст программы'),
  p('Ниже приведён полный текст программы (без автоматических тестов). Файлы расположены в порядке: сборка и модуль, модель, контроллер, вспомогательный класс, консольная версия, оконная версия.'),
  ...appendixFiles.flatMap(codeFile),
];

// ---------- документ ----------

const doc = new Document({
  creator: 'А. А. Минязов',
  title: 'Отчёт о лабораторной работе № 1 «Искусственная жизнь»',
  features: process.env.NO_UPDATE ? undefined : { updateFields: true },
  styles: {
    default: { document: { run: { font: FONT, size: 28 }, paragraph: { spacing: { line: 360 } } } },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: FONT, size: 32, bold: true }, paragraph: { outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: FONT, size: 28, bold: true }, paragraph: { outlineLevel: 1 } },
      // Строки оглавления — одинарный интервал, чтобы содержание помещалось на одну страницу
      { id: 'TOC1', name: 'toc 1', basedOn: 'Normal', next: 'Normal',
        run: { font: FONT, size: 28 }, paragraph: { spacing: { line: 276, before: 0, after: 60 } } },
      { id: 'TOC2', name: 'toc 2', basedOn: 'Normal', next: 'Normal',
        run: { font: FONT, size: 28 }, paragraph: { spacing: { line: 276, before: 0, after: 60 }, indent: { left: 280 } } },
    ],
  },
  numbering: {
    config: [{
      reference: 'dash',
      levels: [{ level: 0, format: LevelFormat.BULLET, text: '–', alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 1069, hanging: 360 } } } }],
    }],
  },
  sections: [
    { properties: { ...portrait, titlePage: false }, children: titlePage },
    { properties: portrait, footers: footer(), children: [...tocPage, ...chapter1, ...chapter2, ...chapter3] },
    { properties: landscape, footers: footer(), children: landscapePart },
    { properties: portrait, footers: footer(), children: [...chapter3end, ...chapter4, ...chapter5, ...conclusion, ...appendix] },
  ],
});

fs.mkdirSync(path.dirname(OUT), { recursive: true });
Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync(OUT, buf);
  console.log('готово:', OUT, Math.round(buf.length / 1024), 'КБ; рисунков', figureNo, ', таблиц', tableNo);
});

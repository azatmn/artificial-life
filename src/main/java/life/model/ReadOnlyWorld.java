package life.model;

/** Поле «только для чтения»: экран может его рисовать, но не может ничего на нём менять. */
public interface ReadOnlyWorld {

    int getWidth();

    int getHeight();

    /** Житель в клетке или null, если клетка пустая. */
    Agent getAgent(int x, int y);
}

package tz.base.common;

/**
 * Tuple
 *
 * @param <F> first item
 * @param <S> second item
 */
public class Tuple<F, S>
{
    private F first;
    private S second;

    /**
     * Create new Tuple
     *
     * @param first  first item
     *
     * @param second second item
     *
     */
    public Tuple(F first, S second)
    {
        this.first  = first;
        this.second = second;
    }

    /**
     * Get first item
     *
     * @return  first item
     */
    public F getFirst()
    {
        return first;
    }

    /**
     * Set first item
     *
     * @param first
     *        Set first item
     *
     */
    public void setFirst(F first)
    {
        this.first = first;
    }

    /**
     * Get second item
     *
     * @return  second item
     */
    public S getSecond()
    {
        return second;
    }

    /**
     * Set second item
     *
     * @param second
     *        Set second item
     *
     */
    public void setSecond(S second)
    {
        this.second = second;
    }
}

package net.forestany.mediacollection.search.data;

public class Season {

    /* Fields */

    public String AirDate;
    public int EpisodeCount;
    public int Id;
    public String Name;
    public String Overview;
    public String PosterPath;
    public int SeasonNumber;
    public double VoteAverage;

    /* Properties */

    /* Methods */

    public Season() {

    }

    @Override public String toString() {
        String s_foo = "";

        for (java.lang.reflect.Field o_field : this.getClass().getDeclaredFields()) {
            if (o_field.getName().startsWith("this$")) {
                continue;
            }

            try {
                s_foo += o_field.getName() + " = " + o_field.get(this).toString() + "|";
            } catch (Exception o_exc) {
                s_foo += o_field.getName() + " = null|";
            }
        }

        s_foo = s_foo.substring(0, s_foo.length() - 1);

        return s_foo;
    }
}

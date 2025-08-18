package net.forestany.mediacollection.search.data;

public class MovieSearchResult {

    /* Fields */

    public boolean Adult;
    public String BackdropPath;
    public long[] GenreIds;
    public int Id;
    public String OriginalLanguage;
    public String OriginalTitle;
    public String Overview;
    public double Popularity;
    public String PosterPath;
    public String ReleaseDate;
    public String Title;
    public boolean Video;
    public double VoteAverage;
    public int VoteCount;

    /* Properties */

    /* Methods */

    public MovieSearchResult() {

    }

    @Override public String toString() {
        String s_foo = "";

        for (java.lang.reflect.Field o_field : this.getClass().getDeclaredFields()) {
            if (o_field.getName().startsWith("this$")) {
                continue;
            }

            try {
                if (o_field.getType().getTypeName().endsWith("[]")) {
                    Long[] a_foo = new Long[((long[])o_field.get(this)).length];
                    int i = 0;

                    for (long f_foo : (long[])o_field.get(this)) {
                        a_foo[i++] = f_foo;
                    }

                    s_foo += o_field.getName() + " = " + net.forestany.forestj.lib.Helper.printArrayList( java.util.Arrays.asList( (Long[])a_foo ) ) + "|";
                } else {
                    s_foo += o_field.getName() + " = " + o_field.get(this).toString() + "|";
                }
            } catch (Exception o_exc) {
                s_foo += o_field.getName() + " = null|";
            }
        }

        s_foo = s_foo.substring(0, s_foo.length() - 1);

        return s_foo;
    }
}

package net.forestany.mediacollection.search.data;

public class SeriesSearchResults {

    /* Fields */

    public int Page;
    public java.util.List<SeriesSearchResult> Results = new java.util.ArrayList<SeriesSearchResult>();
    public int TotalPages;
    public int TotalResults;

    /* Properties */

    /* Methods */

    public SeriesSearchResults() {

    }

    @Override public String toString() {
        String s_foo = "";

        for (java.lang.reflect.Field o_field : this.getClass().getDeclaredFields()) {
            if (o_field.getName().startsWith("this$")) {
                continue;
            }

            try {
                if (o_field.get(this) instanceof java.util.List<?>) {
                    @SuppressWarnings("unchecked")
                    java.util.List<SeriesSearchResult> a_items = (java.util.List<SeriesSearchResult>)o_field.get(this);

                    s_foo += o_field.getName() + " = [";

                    if (a_items.size() > 0) {
                        for (SeriesSearchResult o_foo : a_items) {
                            s_foo += o_foo.toString() + ",";
                        }

                        s_foo = s_foo.substring(0, s_foo.length() - 1);
                    }

                    s_foo += "]|";
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
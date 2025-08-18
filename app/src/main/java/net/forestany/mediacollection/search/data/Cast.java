package net.forestany.mediacollection.search.data;

public class Cast {

    /* Fields */

    public boolean Adult;
    public int Gender;
    public int Id;
    public String KnownForDepartment;
    public String Name;
    public String OriginalName;
    public double Popularity;
    public String ProfilePath;
    public int CastId;
    public String Character;
    public String CreditId;
    public int Order;

    /* Properties */

    /* Methods */

    public Cast() {

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

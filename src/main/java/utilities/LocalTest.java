package utilities;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;

public class LocalTest {

    public static void main(String[] args){
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.FACILITY.name());
        System.out.println("api.retrieveAll() ->  call");
        api.retrieveAll().forEach(item -> {
            System.out.println(item);
        });
    }

}

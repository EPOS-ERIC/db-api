package utilities;

import org.epos.eposdatamodel.LinkedEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperationWebserviceInDistributionSingleton {

    private static OperationWebserviceInDistributionSingleton instance;

    private Map<LinkedEntity, LinkedEntity> relations;

    public static OperationWebserviceInDistributionSingleton getInstance() {
        if (instance == null) {
            instance = new OperationWebserviceInDistributionSingleton();
        }
        return instance;
    }

    private OperationWebserviceInDistributionSingleton() {
        relations = new HashMap<>();
    }

    public void createRelation(LinkedEntity source, LinkedEntity target){
        //System.out.println(source+" "+target);
        relations.put(source, target);
    }

    public LinkedEntity getTarget(LinkedEntity source) {
        LinkedEntity target = relations.get(source);
        relations.remove(source);
        return target;
    }
}

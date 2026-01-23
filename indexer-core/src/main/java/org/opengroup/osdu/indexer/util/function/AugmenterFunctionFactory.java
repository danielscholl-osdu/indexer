package org.opengroup.osdu.indexer.util.function;


import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.indexer.model.indexproperty.ValueExtraction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AugmenterFunctionFactory {
    private List<IAugmenterFunction> augmenterFunctionList;

    @Autowired
    public AugmenterFunctionFactory(List<IAugmenterFunction> augmenterFunctionList) {
        this.augmenterFunctionList = augmenterFunctionList;
    }

    public boolean isAugmenterFunction(ValueExtraction valueExtraction) {
        return getAugmenterFunction(valueExtraction) != null;
    }

    public IAugmenterFunction getAugmenterFunction(ValueExtraction valueExtraction) {
        for(IAugmenterFunction augmenterFunction : this.augmenterFunctionList) {
            if(augmenterFunction.isMatched(valueExtraction)) {
                return augmenterFunction;
            }
        }
        return null;
    }
}

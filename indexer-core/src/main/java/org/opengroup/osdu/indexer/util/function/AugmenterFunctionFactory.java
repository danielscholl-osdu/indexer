package org.opengroup.osdu.indexer.util.function;


import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.indexer.model.indexproperty.ValueExtraction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AugmenterFunctionFactory {
    private final ExtentAugmenterImpl extentAugmenter;
    private final LengthAugmenterImpl lengthAugmenter;
    private final AreaAugmenterImpl areaAugmenter;

    private List<IAugmenterFunction> augmenterFunctionList;

    public boolean isAugmenterFunction(ValueExtraction valueExtraction) {
        return getAugmenterFunction(valueExtraction) != null;
    }

    public IAugmenterFunction getAugmenterFunction(ValueExtraction valueExtraction) {
        for(IAugmenterFunction augmenterFunction : getAugmenterFunctionList()) {
            if(augmenterFunction.isMatched(valueExtraction)) {
                return augmenterFunction;
            }
        }
        return null;
    }

    private synchronized List<IAugmenterFunction> getAugmenterFunctionList() {
        if (augmenterFunctionList == null) {
            augmenterFunctionList = List.of(extentAugmenter, lengthAugmenter, areaAugmenter);
        }
        return augmenterFunctionList;
    }
}

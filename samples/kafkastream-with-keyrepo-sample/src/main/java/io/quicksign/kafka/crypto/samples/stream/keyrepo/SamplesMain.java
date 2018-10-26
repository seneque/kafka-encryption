/*-
 * #%L
 * Kafka Encryption
 * %%
 * Copyright (C) 2018 Quicksign
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.quicksign.kafka.crypto.samples.stream.keyrepo;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.quicksign.kafka.crypto.encryption.KeyProvider;
import io.quicksign.kafka.crypto.generatedkey.MasterKeyEncryption;
import io.quicksign.kafka.crypto.keyrepository.KeyRepository;
import io.quicksign.kafka.crypto.keyrepository.RepositoryBasedKeyProvider;
import io.quicksign.kafka.crypto.keyrepository.RepositoryBasedKeyReferenceExtractor;
import io.quicksign.kafka.crypto.pairing.keyextractor.KeyReferenceExtractor;

public class SamplesMain {

    public static void main(String... args) {


        // tag::keyRepository[]
        KeyRepository keyRepository = new KeyStoreBasedKeyRepository(
                new File("/tmp/sample.pkcs12"),
                "sample"
        );
        // end::keyRepository[]

        KeyProvider keyProvider = new RepositoryBasedKeyProvider(keyRepository, new SampleKeyNameObfuscator());

        // We create an encryption keyref for each record's key.
        // Two records with the same record key have the same encryption key ref.
        KeyReferenceExtractor keyReferenceExtractor = new RepositoryBasedKeyReferenceExtractor(new SampleKeyNameExtractor(), new SampleKeyNameObfuscator());


        ExecutorService executorService = Executors.newFixedThreadPool(3);

        for (Modules activeModule : modules) {
            Runnable task = activeModule.getBuilder().apply(masterKeyEncryption);
            executorService.submit(task);
        }


    }


    private enum Modules {
        PRODUCER("--producer", mk -> new SampleProducer(mk)),
        CONSUMER("--consumer", mk -> new SampleDecryptingConsumer(mk)),
        RAW_CONSUMER("--rawConsumer", mk -> new SampleRawConsumer());

        private final String option;
        private final Function<MasterKeyEncryption, Runnable> builder;

        Modules(String option, Function<MasterKeyEncryption, Runnable> builder) {
            this.option = option;
            this.builder = builder;
        }

        public static Optional<Modules> getByOption(String option) {
            return Arrays.stream(values()).filter(m -> m.option.equals(option)).findFirst();
        }

        public Function<MasterKeyEncryption, Runnable> getBuilder() {
            return builder;
        }
    }
}

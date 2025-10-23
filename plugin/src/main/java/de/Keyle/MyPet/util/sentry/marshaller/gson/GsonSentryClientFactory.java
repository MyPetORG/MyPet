/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.sentry.marshaller.gson;

import de.Keyle.MyPet.util.sentry.marshaller.gson.bindings.*;
import io.sentry.DefaultSentryClientFactory;
import io.sentry.dsn.Dsn;
import io.sentry.event.interfaces.*;
import io.sentry.marshaller.Marshaller;

public class GsonSentryClientFactory extends DefaultSentryClientFactory {

    protected Marshaller createMarshaller(Dsn dsn) {
        int maxMessageLength = getMaxMessageLength(dsn);
        GsonMarshaller marshaller = createGsonMarshaller(maxMessageLength);

        // Set GSON marshaller bindings
        StackTraceInterfaceBinding stackTraceBinding = new StackTraceInterfaceBinding();
        // Enable common frames hiding unless its value is 'false'.
        stackTraceBinding.setRemoveCommonFramesWithEnclosing(getHideCommonFramesEnabled(dsn));
        stackTraceBinding.setInAppFrames(getInAppFrames(dsn));

        marshaller.addInterfaceBinding(StackTraceInterface.class, stackTraceBinding);
        marshaller.addInterfaceBinding(ExceptionInterface.class, new ExceptionInterfaceBinding(stackTraceBinding));
        marshaller.addInterfaceBinding(MessageInterface.class, new MessageInterfaceBinding(maxMessageLength));
        marshaller.addInterfaceBinding(UserInterface.class, new UserInterfaceBinding());
        marshaller.addInterfaceBinding(DebugMetaInterface.class, new DebugMetaInterfaceBinding());
        // Enable compression unless the option is set to false
        marshaller.setCompression(getCompressionEnabled(dsn));

        return marshaller;
    }

    protected GsonMarshaller createGsonMarshaller(int maxMessageLength) {
        return new GsonMarshaller(maxMessageLength);
    }
}

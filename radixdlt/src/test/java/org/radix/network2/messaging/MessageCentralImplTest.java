package org.radix.network2.messaging;

import com.radixdlt.serialization.DsonOutput.Output;
import com.radixdlt.serialization.Serialization;
import com.radixdlt.universe.Universe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.radix.events.Events;
import org.radix.modules.Modules;
import org.radix.network.messages.TestMessage;
import org.radix.network.messaging.Message;
import org.radix.network2.messaging.MessagingDummyConfigurations.DummyTransport;
import org.radix.network2.messaging.MessagingDummyConfigurations.DummyTransportOutboundConnection;
import org.radix.network2.addressbook.AddressBook;
import org.radix.network2.addressbook.Peer;
import org.radix.network2.transport.StaticTransportMetadata;
import org.radix.network2.transport.TransportInfo;
import org.radix.properties.RuntimeProperties;
import org.radix.universe.system.events.QueueFullEvent;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageCentralImplTest {

	private Serialization serialization;
	private DummyTransportOutboundConnection toc;
	private DummyTransport dt;
	private TransportManager transportManager;
	private MessageCentralImpl mci;
	private PriorityBlockingQueue<MessageEvent> inboundQueue;
	private PriorityBlockingQueue<MessageEvent> outboundQueue;
	private Events events;

	@Before
	public void testSetup() {
		this.serialization = Serialization.getDefault();
		MessageCentralConfiguration conf = new MessagingDummyConfigurations.DummyMessageCentralConfiguration();

		// Curse you singletons
		Universe universe = mock(Universe.class);
		when(universe.getMagic()).thenReturn(0);
		RuntimeProperties runtimeProperties = mock(RuntimeProperties.class);
		when(runtimeProperties.get(eq("network.whitelist"), any())).thenReturn("");
		AddressBook addressbook = mock(AddressBook.class);
		when(addressbook.peer(any(TransportInfo.class))).thenReturn(mock(Peer.class));

		Modules.put(Universe.class, universe);
		Modules.put(RuntimeProperties.class, runtimeProperties);
		Modules.put(AddressBook.class, addressbook);

		// Other scaffolding
		this.toc = new DummyTransportOutboundConnection();
		this.dt = new DummyTransport(this.toc);

		this.transportManager = new MessagingDummyConfigurations.DummyTransportManager(this.dt);

		this.events = mock(Events.class);
		inboundQueue = spy(new PriorityBlockingQueue<>(conf.messagingInboundQueueMax(0)));
		outboundQueue = spy(new PriorityBlockingQueue<>(conf.messagingOutboundQueueMax(0)));
		EventQueueFactory<MessageEvent> queueFactory = eventQueueFactoryMock();
		doReturn(inboundQueue).when(queueFactory).createEventQueue(conf.messagingInboundQueueMax(0));
		doReturn(outboundQueue).when(queueFactory).createEventQueue(conf.messagingOutboundQueueMax(0));
		this.mci = new MessageCentralImpl(new MessagingDummyConfigurations.DummyMessageCentralConfiguration(), serialization, transportManager, events, System::currentTimeMillis,
				queueFactory);
	}

	@After
	public void cleanup() {
		Modules.remove(Universe.class);
		Modules.remove(RuntimeProperties.class);
		Modules.remove(AddressBook.class);

		this.mci.close();
	}

	@Test
	public void testConstruct() {
		// Make sure start called on our transport
		assertNotNull(dt.getMessageSink());
	}

	@Test
	public void testClose() {
		mci.close();
		assertTrue(dt.isClosed());
	}

	@Test
	public void testSend() throws InterruptedException {
		Message msg = new TestMessage();
		Peer peer = mock(Peer.class);
		mci.send(peer, msg);
		assertTrue(toc.getCountDownLatch().await(10, TimeUnit.SECONDS));
		assertTrue(toc.isSent());
	}

	@Test
	public void testSendMessageDeliveredToTransport() throws InterruptedException {
		Message msg = new TestMessage();
		Peer peer = mock(Peer.class);

		int numberOfRequests = 6;
		CountDownLatch receivedFlag = new CountDownLatch(numberOfRequests);
		toc.setCountDownLatch(receivedFlag);
		for (int i = 0; i < numberOfRequests; i++) {
			mci.send(peer, msg);
		}

		receivedFlag.await(10, TimeUnit.SECONDS);
		assertEquals(numberOfRequests, toc.getMessages().size());
	}

	@Test
	public void testInjectMessageDeliveredToListeners() throws InterruptedException {
		Message msg = spy(new TestMessage());
		doReturn(System.currentTimeMillis()).when(msg).getTimestamp();
		Peer peer = mock(Peer.class);

		int numberOfRequests = 6;
		CountDownLatch receivedFlag = new CountDownLatch(numberOfRequests);
		List<Message> messages = new ArrayList<>();
		mci.addListener(TestMessage.class, (source, message) -> {
			messages.add(message);
			receivedFlag.countDown();
		});

		for (int i = 0; i < numberOfRequests; i++) {
			mci.inject(peer, msg);
		}
		receivedFlag.await(10, TimeUnit.SECONDS);
		assertEquals(numberOfRequests, messages.size());
		verify(inboundQueue, times(numberOfRequests)).offer(any());
	}

	@Test
	public void testInjectQueueIsFull() throws Exception {
		testQueueIsFull(inboundQueue, (peer, message) -> mci.inject(peer, message));
	}

	@Test
	public void testSendQueueIsFull() throws Exception {
		testQueueIsFull(outboundQueue, (peer, message) -> mci.send(peer, message));
	}

	private <T> void testQueueIsFull(Queue<T> queue, BiConsumer<Peer, Message> biConsumer) {
		doReturn(false).when(queue).offer(notNull());
		Message msg = new TestMessage();
		Peer peer = mock(Peer.class);

		int numberOfRequests = 6;
		for (int i = 0; i < numberOfRequests; i++) {
			biConsumer.accept(peer, msg);
		}
		verify(events, times(numberOfRequests)).broadcast(any(QueueFullEvent.class));
	}

	@Test
	public void testInbound() throws IOException, InterruptedException {
		Message msg = new TestMessage();
		byte[] data = Snappy.compress(serialization.toDson(msg, Output.WIRE));

		AtomicReference<Message> receivedMessage = new AtomicReference<>();
		Semaphore receivedFlag = new Semaphore(0);

		mci.addListener(msg.getClass(), (peer, messsage) -> {
			receivedMessage.set(messsage);
			receivedFlag.release();
		});

		TransportInfo source = TransportInfo.of("DUMMY", StaticTransportMetadata.empty());

		InboundMessage message = InboundMessage.of(source, data);
		dt.inboundMessage(message);

		assertTrue(receivedFlag.tryAcquire(10, TimeUnit.SECONDS));
		assertNotNull(receivedMessage.get());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddNullListener() {
		mci.addListener(TestMessage.class, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddListenerTwice() {
		MessageListener<TestMessage> listener = (source, message) -> {};
		mci.addListener(TestMessage.class, listener);
		mci.addListener(TestMessage.class, listener);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemoveNullListener() {
		mci.removeListener(TestMessage.class, null);
	}

	@Test
	public void testAddRemoveListener() {
		MessageListener<TestMessage> listener1 = (source, message) -> {};
		MessageListener<TestMessage> listener2 = (source, message) -> {};

		mci.addListener(TestMessage.class, listener1);
		assertEquals(1, mci.listenersSize());

		mci.addListener(TestMessage.class, listener2);
		assertEquals(2, mci.listenersSize());

		mci.removeListener(TestMessage.class, listener1);
		assertEquals(1, mci.listenersSize());

		mci.removeListener(TestMessage.class, listener1);
		assertEquals(1, mci.listenersSize());

		mci.removeListener(TestMessage.class, listener2);
		assertEquals(0, mci.listenersSize());
	}

	@Test
	public void testRemoveUnspecifiedListener() {
		MessageListener<TestMessage> listener1 = (source, message) -> {};
		MessageListener<TestMessage> listener2 = (source, message) -> {};

		mci.addListener(TestMessage.class, listener1);
		assertEquals(1, mci.listenersSize());

		mci.addListener(TestMessage.class, listener2);
		assertEquals(2, mci.listenersSize());

		mci.removeListener(listener1);
		assertEquals(1, mci.listenersSize());

		mci.removeListener(listener1);
		assertEquals(1, mci.listenersSize());

		mci.removeListener(listener2);
		assertEquals(0, mci.listenersSize());
	}

	@SuppressWarnings("unchecked")
	private <T> EventQueueFactory<T> eventQueueFactoryMock() {
		return mock(EventQueueFactory.class);
	}
}
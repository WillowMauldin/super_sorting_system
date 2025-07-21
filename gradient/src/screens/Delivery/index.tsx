import { useEffect, useState } from 'react';
import {
  ActionController,
  ItemSelector,
  NodeSelector,
  SelectedItems,
  SelectedItemsFinal,
} from '../../common';
import styled from 'styled-components';

/*
 *
 * Steps:
 * Select Items
 * Confirm Items + Delivery Destination
 *
 */

type Props = {
  actionController: ActionController;
};

export const Delivery = ({ actionController }: Props) => {
  const [selectedItems, setSelectedItems] = useState<SelectedItems>({});
  const [selectedItemsFinal, setSelectedItemsFinal] =
    useState<SelectedItemsFinal | null>(null);

  const back = (clear?: boolean) => {
    setSelectedItemsFinal(null);

    if (clear) setSelectedItems({});
  };

  useEffect(() => {
    const handler = (ev: KeyboardEvent) => {
      if (selectedItemsFinal && ev.key === 'Escape') {
        back();
      }
    };

    document.addEventListener('keydown', handler);

    return () => document.removeEventListener('keydown', handler);
  });

  if (!selectedItemsFinal)
    return (
      <ItemSelector
        selectedItems={selectedItems}
        setSelectedItems={setSelectedItems}
        submit={(itemsFinal) => setSelectedItemsFinal(itemsFinal)}
      />
    );

  const deliver = (node: string) => {
    actionController.deliverItems(node, selectedItemsFinal);
    back(true);
  };

  return (
    <Container>
      <h2>Finalize Delivery</h2>
      {selectedItemsFinal.length === 0 ? (
        <>
          <p>You did not select any items!</p>
        </>
      ) : (
        <>
          <p>Selected items:</p>
          <ul>
            {selectedItemsFinal.map((item) => (
              <li key={item.item.stackable_hash}>
                {item.item.prettyPrinted} {item.shulkerCount > 0 
                  ? `x${item.shulkerCount} shulkers` 
                  : `x${item.itemCount} items`}
              </li>
            ))}
          </ul>
          <p>Deliver to:</p>
          <NodeSelector submit={deliver} purpose="delivery" />
        </>
      )}
      <button onClick={() => back(false)}>Back</button>
    </Container>
  );
};

const Container = styled.div`
  display: flex;
  flex-direction: column;

  & > button {
    margin-top: 1em;
  }
`;
